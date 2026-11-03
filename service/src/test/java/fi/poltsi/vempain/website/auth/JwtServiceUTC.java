package fi.poltsi.vempain.website.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        // half of the 60 second lifetime is left at the earliest after 30 seconds: a fresh token is not due yet
        assertTrue(service.refresh(new AuthenticatedUser(7, "alice", false, token))
            .isEmpty());
        service.revoke(token);
        verify(repository).deleteByToken(token);
    }

    @Test
    void theSessionSlidesOnceTheThresholdIsReachedAndTheOldTokenGetsAGracePeriod() {
        var start = Instant.parse("2026-10-31T10:10:09Z");
        var clock = new MutableClock(start);
        var timed = new JwtService(repository, properties, new ObjectMapper(), clock);
        String token = timed.issueToken(7, "alice", true);
        verify(repository).save(any(WebSiteJwtToken.class));

        // 29 seconds in: more than half of the 60 second lifetime is left, nothing happens
        clock.advance(29);
        assertTrue(timed.refresh(new AuthenticatedUser(7, "alice", true, token))
            .isEmpty());
        verify(repository, never()).shortenExpiry(any(), any());

        // 31 seconds in: a fresh token is issued and the old one ends after the grace period
        clock.advance(2);
        properties.setJwtRefreshGraceSeconds(45);
        String fresh = timed.refresh(new AuthenticatedUser(7, "alice", true, token))
            .orElseThrow();
        assertNotEquals(token, fresh);
        assertEquals(start.plusSeconds(31 + 60)
            .getEpochSecond(), timed.expiresAt(fresh));
        verify(repository, org.mockito.Mockito.times(2)).save(any(WebSiteJwtToken.class));
        verify(repository).shortenExpiry(token, LocalDateTime.ofInstant(start.plusSeconds(31 + 45), ZoneId.of("UTC")));
    }

    @Test
    void theSameTokenIsRecordedOnceWithinASecond() {
        var clock = new MutableClock(Instant.parse("2026-10-31T10:10:09.500Z"));
        var timed = new JwtService(repository, properties, new ObjectMapper(), clock);
        String token = timed.issueToken(7, "alice", true);
        when(repository.existsByToken(token)).thenReturn(true);

        // The requests a page fires in parallel sign to the same token within the same second: no second row, no shortened expiry
        assertEquals(token, timed.issueToken(7, "alice", true));
        verify(repository, org.mockito.Mockito.times(1)).save(any(WebSiteJwtToken.class));
        properties.setJwtRefreshThresholdSeconds(60);
        assertEquals(Optional.of(token), timed.refresh(new AuthenticatedUser(7, "alice", true, token)));
        verify(repository, never()).shortenExpiry(any(), any());
    }

    @Test
    void refreshThresholdAndGraceFollowTheConfiguration() {
        assertEquals(30, service.refreshThresholdSeconds(), "half of the lifetime by default");
        properties.setJwtRefreshThresholdSeconds(20);
        assertEquals(20, service.refreshThresholdSeconds());
        properties.setJwtRefreshThresholdSeconds(500);
        assertEquals(60, service.refreshThresholdSeconds(), "never more than the lifetime");
        properties.setJwtRefreshGraceSeconds(-5);
        assertEquals(0, service.refreshGraceSeconds());
        assertEquals(0, service.expiresAt("not.a.jwt"));
        assertEquals(0, service.expiresAt(null));
    }

    /** A clock the tests move forward by hand */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
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
