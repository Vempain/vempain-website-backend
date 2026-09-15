package fi.poltsi.vempain.website.auth;

import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.entity.WebSiteJwtToken;
import fi.poltsi.vempain.website.repository.WebSiteJwtTokenRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtServiceUTC {
    private final WebSiteJwtTokenRepository repository = mock(WebSiteJwtTokenRepository.class);
    private final SiteProperties properties = new SiteProperties();
    private final JwtService service;
    JwtServiceUTC() { properties.setJwtSecret("test-secret"); properties.setJwtTtlSeconds(60); service = new JwtService(repository, properties, new ObjectMapper()); }

    @Test void issueVerifyRefreshAndRevokeRoundTrip() {
        String token = service.issueToken(7, "alice", true);
        assertEquals(7, service.verify(token).orElseThrow().userId());
        assertTrue(service.verify(token).orElseThrow().globalPermission());
        verify(repository).save(any(WebSiteJwtToken.class));
        String refreshed = service.refresh(new AuthenticatedUser(7, "alice", false, token));
        assertNotEquals(token, refreshed);
        service.revoke(token); verify(repository).deleteByToken(token);
    }

    @Test void malformedAndTamperedTokensAreRejected() {
        assertTrue(service.verify(null).isEmpty());
        assertTrue(service.verify("a.b.c").isEmpty());
        String token = service.issueToken(1, "u", false);
        assertTrue(service.verify(token + "x").isEmpty());
    }

	@Test
	void missingSecretCannotFallBackToAWellKnownSigningKey() {
		properties.setJwtSecret("");
		assertThrows(IllegalStateException.class, () -> service.issueToken(1, "u", false));
	}

    @Test void persistedValidityAndTtlFallbackWork() {
        properties.setJwtTtlSeconds(0);
        assertEquals(1200, service.ttlSeconds());
        when(repository.findValidToken(eq("t"), any(LocalDateTime.class))).thenReturn(Optional.of(new WebSiteJwtToken(9L, "t", LocalDateTime.now(), LocalDateTime.now().plusMinutes(1))));
        assertEquals(Optional.of(9L), service.tokenOwner("t"));
        assertTrue(service.isPersistedAndValid("t"));
        assertTrue(service.tokenOwner("").isEmpty());
    }
}
