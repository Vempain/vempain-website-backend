package fi.poltsi.vempain.website.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.entity.WebSiteJwtToken;
import fi.poltsi.vempain.website.exception.TokenExpiredException;
import fi.poltsi.vempain.website.repository.WebSiteJwtTokenRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Issues and verifies the HS256 tokens used by the site. The implementation is intentionally self contained so that no JWT library is needed.
 */
@Service
public class JwtService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HEADER_JSON = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}";

    private final WebSiteJwtTokenRepository jwtTokenRepository;
    private final SiteProperties siteProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JwtService(WebSiteJwtTokenRepository jwtTokenRepository, SiteProperties siteProperties, ObjectMapper objectMapper) {
        this(jwtTokenRepository, siteProperties, objectMapper, Clock.systemDefaultZone());
    }

    JwtService(WebSiteJwtTokenRepository jwtTokenRepository, SiteProperties siteProperties, ObjectMapper objectMapper, Clock clock) {
        this.clock = clock;
        this.jwtTokenRepository = jwtTokenRepository;
        this.siteProperties = siteProperties;
        this.objectMapper = objectMapper;
    }

    private static long longClaim(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text
            && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        return fallback;
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value);
    }

    /**
     * Creates a token for the given user and records it in the database so that it can be invalidated on logout.
     */
    @Transactional
    public String issueToken(long userId, String username, boolean globalPermission) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds());

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", userId);
        claims.put("username", username);
        claims.put("global_permission", globalPermission);
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String token = sign(claims);
        // The claims carry whole seconds, so two issues within one second sign to the same token; it is recorded once
        if (!jwtTokenRepository.existsByToken(token)) {
            jwtTokenRepository.save(new WebSiteJwtToken(userId, token, LocalDateTime.ofInstant(issuedAt, clock.getZone()),
                    LocalDateTime.ofInstant(expiresAt, clock.getZone())));
        }

        return token;
    }

    /**
     * Verifies signature and expiry of the token.
     *
     * @return the claims, or an empty optional when the token is missing or malformed
     * @throws TokenExpiredException when the token is valid but expired
     */
    public Optional<AuthenticatedUser> verify(String token) {
        if (token == null
            || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            byte[] expected = hmac(parts[0] + "." + parts[1]);
            byte[] provided = Base64.getUrlDecoder()
                .decode(parts[2]);
            if (!MessageDigest.isEqual(expected, provided)) {
                LOG.debug("Rejected a token with an invalid signature");
                return Optional.empty();
            }

            Map<String, Object> claims = readClaims(parts[1]);
            long expiresAt = longClaim(claims.get("exp"), 0L);
            if (expiresAt > 0L
                && Instant.now(clock)
                    .getEpochSecond() >= expiresAt) {
                throw new TokenExpiredException("Session expired");
            }

            Object subject = claims.containsKey("sub") ? claims.get("sub") : claims.get("id");
            Object username = claims.get("username");

            return Optional.of(new AuthenticatedUser(longClaim(subject, AuthenticatedUser.ANONYMOUS_USER_ID),
                    username == null ? null : String.valueOf(username), Boolean.TRUE.equals(claims.get("global_permission")), token));
        } catch (TokenExpiredException expired) {
            throw expired;
        } catch (RuntimeException | GeneralSecurityException failure) {
            LOG.debug("Ignoring a malformed token: {}", failure.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Slides the session window: once the token has less than the refresh threshold left (half of the lifetime by default), a fresh token is issued for the
     * same claims and the old one is shortened to the grace period, so that the requests a page fires in parallel with the old cookie still pass while the old
     * token cannot outlive the new one. Earlier in the lifetime nothing happens, which keeps one row per session in the database instead of one per request.
     *
     * @return the fresh token to put into the cookie, or empty when the current one is still good
     */
    @Transactional
    public Optional<String> refresh(AuthenticatedUser user) {
        long now = Instant.now(clock)
            .getEpochSecond();
        long remaining = expiresAt(user.token()) - now;
        if (remaining > refreshThresholdSeconds()) {
            return Optional.empty();
        }
        String fresh = issueToken(user.userId(), user.username(), user.globalPermission());
        if (!fresh.equals(user.token())) {
            LocalDateTime graceEnd = LocalDateTime.ofInstant(Instant.ofEpochSecond(now + refreshGraceSeconds()), clock.getZone());
            jwtTokenRepository.shortenExpiry(user.token(), graceEnd);
        }
        return Optional.of(fresh);
    }

    /** The {@code exp} claim of a token in epoch seconds, 0 when it cannot be read */
    long expiresAt(String token) {
        if (token == null) {
            return 0L;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return 0L;
        }
        try {
            return longClaim(readClaims(parts[1]).get("exp"), 0L);
        } catch (RuntimeException failure) {
            return 0L;
        }
    }

    /** Seconds of remaining lifetime below which the session slides; the configured value, else half of the lifetime */
    public long refreshThresholdSeconds() {
        long configured = siteProperties.getJwtRefreshThresholdSeconds();
        return configured > 0L ? Math.min(configured, ttlSeconds()) : ttlSeconds() / 2;
    }

    /** How long a replaced token stays valid after a refresh */
    public long refreshGraceSeconds() {
        return Math.max(0L, siteProperties.getJwtRefreshGraceSeconds());
    }

    @Transactional
    public void revoke(String token) {
        if (token != null
            && !token.isBlank()) {
            jwtTokenRepository.deleteByToken(token);
        }
    }

    public boolean isPersistedAndValid(String token) {
        return tokenOwner(token).isPresent();
    }

    /**
     * @return the user the token was issued for, provided the token is still recorded and has not expired
     */
    public Optional<Long> tokenOwner(String token) {
        if (token == null
            || token.isBlank()) {
            return Optional.empty();
        }

        return jwtTokenRepository.findValidToken(token, LocalDateTime.now(clock))
            .map(WebSiteJwtToken::getUserId);
    }

    public long ttlSeconds() {
        long ttl = siteProperties.getJwtTtlSeconds();
        return ttl > 0L ? ttl : 1200L;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readClaims(String encodedPayload) {
        return objectMapper.readValue(Base64.getUrlDecoder()
            .decode(encodedPayload), Map.class);
    }

    private String sign(Map<String, Object> claims) {
        try {
            String header = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String payload = base64Url(objectMapper.writeValueAsBytes(claims));
            String signature = base64Url(hmac(header + "." + payload));

            return header + "." + payload + "." + signature;
        } catch (Exception failure) {
            throw new IllegalStateException("Unable to sign the authentication token", failure);
        }
    }

    private byte[] hmac(String signingInput) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret(), HMAC_ALGORITHM));

        return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] secret() {
        String secret = siteProperties.getJwtSecret();
        if (secret == null
            || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }

        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
