package fi.poltsi.vempain.website.auth;

import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.entity.WebSiteJwtToken;
import fi.poltsi.vempain.website.exception.TokenExpiredException;
import fi.poltsi.vempain.website.repository.WebSiteJwtTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Issues and verifies the HS256 tokens used by the site. The implementation is
 * intentionally self contained so that no JWT library is needed.
 */
@Service
public class JwtService {

	private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String HEADER_JSON    = "{\"typ\":\"JWT\",\"alg\":\"HS256\"}";

	private final WebSiteJwtTokenRepository jwtTokenRepository;
	private final SiteProperties            siteProperties;
	private final ObjectMapper              objectMapper;

	public JwtService(WebSiteJwtTokenRepository jwtTokenRepository,
	                  SiteProperties siteProperties,
	                  ObjectMapper objectMapper) {
		this.jwtTokenRepository = jwtTokenRepository;
		this.siteProperties     = siteProperties;
		this.objectMapper       = objectMapper;
	}

	private static long longClaim(Object value, long fallback) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value instanceof String text && !text.isBlank()) {
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
	 * Creates a token for the given user and records it in the database so that it can be
	 * invalidated on logout.
	 */
	@Transactional
	public String issueToken(long userId, String username, boolean globalPermission) {
		Instant issuedAt  = Instant.now();
		Instant expiresAt = issuedAt.plusSeconds(ttlSeconds());

		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", userId);
		claims.put("username", username);
		claims.put("global_permission", globalPermission);
		claims.put("iat", issuedAt.getEpochSecond());
		claims.put("exp", expiresAt.getEpochSecond());

		String token = sign(claims);
		jwtTokenRepository.save(new WebSiteJwtToken(
				userId,
				token,
				LocalDateTime.ofInstant(issuedAt, ZoneId.systemDefault()),
				LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault())
		));

		return token;
	}

	/**
	 * Verifies signature and expiry of the token.
	 *
	 * @return the claims, or an empty optional when the token is missing or malformed
	 * @throws TokenExpiredException when the token is valid but expired
	 */
	public Optional<AuthenticatedUser> verify(String token) {
		if (token == null || token.isBlank()) {
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

			Map<String, Object> claims    = readClaims(parts[1]);
			long                expiresAt = longClaim(claims.get("exp"), 0L);
			if (expiresAt > 0L && Instant.now()
			                             .getEpochSecond() >= expiresAt) {
				throw new TokenExpiredException("Session expired");
			}

			Object subject  = claims.containsKey("sub") ? claims.get("sub") : claims.get("id");
			Object username = claims.get("username");

			return Optional.of(new AuthenticatedUser(
					longClaim(subject, AuthenticatedUser.ANONYMOUS_USER_ID),
					username == null ? null : String.valueOf(username),
					Boolean.TRUE.equals(claims.get("global_permission")),
					token
			));
		} catch (TokenExpiredException expired) {
			throw expired;
		} catch (RuntimeException | GeneralSecurityException failure) {
			LOG.debug("Ignoring a malformed token: {}", failure.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Issues a fresh token for the same claims. Used to slide the session window on every
	 * authenticated request.
	 */
	public String refresh(AuthenticatedUser user) {
		return issueToken(user.userId(), user.username(), user.globalPermission());
	}

	@Transactional
	public void revoke(String token) {
		if (token != null && !token.isBlank()) {
			jwtTokenRepository.deleteByToken(token);
		}
	}

	public boolean isPersistedAndValid(String token) {
		return tokenOwner(token).isPresent();
	}

	/**
	 * @return the user the token was issued for, provided the token is still recorded and
	 * has not expired
	 */
	public Optional<Long> tokenOwner(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}

		return jwtTokenRepository.findValidToken(token, LocalDateTime.now())
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
			String header    = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
			String payload   = base64Url(objectMapper.writeValueAsBytes(claims));
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
		if (secret == null || secret.isBlank()) {
			LOG.warn("No JWT secret configured, falling back to an insecure default");
			secret = "secret";
		}

		return secret.getBytes(StandardCharsets.UTF_8);
	}
}
