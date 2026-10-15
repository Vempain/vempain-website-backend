package fi.poltsi.vempain.website.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.entity.WebSiteJwtToken;
import fi.poltsi.vempain.website.exception.TokenExpiredException;
import fi.poltsi.vempain.website.repository.WebSiteJwtTokenRepository;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtServiceUTC {

    private final WebSiteJwtTokenRepository repository = mock(WebSiteJwtTokenRepository.class);
    private final SiteProperties properties = new SiteProperties();
    private final JwtService service;

    JwtServiceUTC() {
        properties.setJwtSecret("test-secret");
        properties.setJwtTtlSeconds(60);
        service = new JwtService(repository, properties, new ObjectMapper());
    }

    @Test
    void issueVerifyRefreshAndRevokeRoundTrip() {
        String token = service.issueToken(7, "alice", true);
        assertEquals(7, service.verify(token)
            .orElseThrow()
            .userId());
        assertTrue(service.verify(token)
            .orElseThrow()
            .globalPermission());
        verify(repository).save(any(WebSiteJwtToken.class));
        String refreshed = service.refresh(new AuthenticatedUser(7, "alice", false, token));
        assertNotEquals(token, refreshed);
        service.revoke(token);
        verify(repository).deleteByToken(token);
    }

    @Test
    void malformedAndTamperedTokensAreRejected() {
        assertTrue(service.verify(null)
            .isEmpty());
        assertTrue(service.verify("a.b.c")
            .isEmpty());
        String token = service.issueToken(1, "u", false);
        assertTrue(service.verify(token + "x")
            .isEmpty());
    }

    @Test
    void missingSecretCannotFallBackToAWellKnownSigningKey() {
        properties.setJwtSecret("");
        assertThrows(IllegalStateException.class, () -> service.issueToken(1, "u", false));
    }

    @Test
    void expiredTokenIsReportedInsteadOfBeingTreatedAsAnonymous() throws Exception {
        long now = Instant.now()
            .getEpochSecond();
        String expired = sign(Map.of("sub", 7, "username", "alice", "exp", now - 10));

        assertThrows(TokenExpiredException.class, () -> service.verify(expired));
    }

    @Test
    void legacyClaimsAreAccepted() throws Exception {
        long now = Instant.now()
            .getEpochSecond();
        String legacy = sign(Map.of("id", "42", "username", "legacy", "exp", String.valueOf(now + 100)));

        AuthenticatedUser user = service.verify(legacy)
            .orElseThrow();

        assertEquals(42L, user.userId());
        assertEquals("legacy", user.username());
        assertFalse(user.globalPermission());
        assertEquals(legacy, user.token());
    }

    @Test
    void unusableSubjectClaimsFallBackToTheAnonymousUser() throws Exception {
        AuthenticatedUser notNumeric = service.verify(sign(Map.of("sub", "not-a-number")))
            .orElseThrow();
        AuthenticatedUser blank = service.verify(sign(Map.of("sub", "   ", "exp", 0)))
            .orElseThrow();
        AuthenticatedUser wrongType = service.verify(sign(Map.of("sub", true, "global_permission", "yes")))
            .orElseThrow();

        assertEquals(AuthenticatedUser.ANONYMOUS_USER_ID, notNumeric.userId());
        assertNull(notNumeric.username());
        assertEquals(AuthenticatedUser.ANONYMOUS_USER_ID, blank.userId());
        assertEquals(AuthenticatedUser.ANONYMOUS_USER_ID, wrongType.userId());
        assertFalse(wrongType.globalPermission());
    }

    @Test
    void tokensWithTheWrongNumberOfSegmentsAreIgnored() {
        assertTrue(service.verify("   ")
            .isEmpty());
        assertTrue(service.verify("header.payload")
            .isEmpty());
        assertTrue(service.verify("a.b.c.d")
            .isEmpty());
    }

    @Test
    void blankTokensAreNotRevoked() {
        service.revoke(null);
        service.revoke(" ");

        verify(repository, never()).deleteByToken(any());
        assertTrue(service.tokenOwner(null)
            .isEmpty());
    }

    /**
     * Signs the claims exactly as the service does, so that the test can craft payloads the service would never issue itself.
     */
    private static String sign(Map<String, Object> claims) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder()
            .withoutPadding();
        String header = encoder.encodeToString("{\"typ\":\"JWT\",\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(new ObjectMapper().writeValueAsBytes(claims));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = encoder.encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));

        return header + "." + payload + "." + signature;
    }

    @Test
    void persistedValidityAndTtlFallbackWork() {
        properties.setJwtTtlSeconds(0);
        assertEquals(1200, service.ttlSeconds());
        when(repository.findValidToken(eq("t"), any(LocalDateTime.class)))
            .thenReturn(Optional.of(new WebSiteJwtToken(9L, "t", LocalDateTime.now(), LocalDateTime.now()
                .plusMinutes(1))));
        assertEquals(Optional.of(9L), service.tokenOwner("t"));
        assertTrue(service.isPersistedAndValid("t"));
        assertTrue(service.tokenOwner("")
            .isEmpty());
    }
}
