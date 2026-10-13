package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.auth.AuthenticatedUser;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceAccessServiceUTC {
    private final AclService acl = mock(AclService.class);
    private final CurrentUserProvider users = mock(CurrentUserProvider.class);
    private final ResourceAccessService service = new ResourceAccessService(acl, users);

    @Test void publicAndGlobalResourcesAreAccessible() {
        assertTrue(service.isAccessible(null));
        when(users.current()).thenReturn(Optional.of(new AuthenticatedUser(1, "a", true, "t")));
		when(acl.canAccess(4L, new AuthenticatedUser(1, "a", true, "t"))).thenReturn(true);
        assertTrue(service.deniedStatus(4L).isEmpty());
		verify(acl).canAccess(4L, new AuthenticatedUser(1, "a", true, "t"));
    }

    @Test void anonymousAndUnauthorizedStatusesAreDistinct() {
        when(users.current()).thenReturn(Optional.empty());
        when(acl.canAccess(4L, null)).thenReturn(false);
        assertEquals(Optional.of(HttpStatus.UNAUTHORIZED), service.deniedStatus(4L));
        AuthenticatedUser user = new AuthenticatedUser(2, "a", false, "t");
        when(users.current()).thenReturn(Optional.of(user));
        assertEquals(Optional.of(HttpStatus.FORBIDDEN), service.deniedStatus(4L));
        when(acl.canAccess(4L, user)).thenReturn(true);
        assertTrue(service.isAccessible(4L));
    }

    @Test void requireAccessThrowsApiException() {
        when(users.current()).thenReturn(Optional.empty());
        when(acl.canAccess(7L, null)).thenReturn(false);
        assertThrows(ApiException.class, () -> service.requireAccess(7L));
    }

	@Test
	void requireAccessDistinguishesAnonymousFromDeniedAndPassesVisibleResources() {
		when(users.current()).thenReturn(Optional.empty());
		when(acl.canAccess(7L, null)).thenReturn(false);
		ApiException anonymous = assertThrows(ApiException.class, () -> service.requireAccess(7L));
		assertEquals(HttpStatus.UNAUTHORIZED, anonymous.getStatus());
		assertEquals("Unauthorized", anonymous.getMessage());

		AuthenticatedUser user = new AuthenticatedUser(2, "a", false, "t");
		when(users.current()).thenReturn(Optional.of(user));
		when(acl.canAccess(7L, user)).thenReturn(false);
		ApiException denied = assertThrows(ApiException.class, () -> service.requireAccess(7L));
		assertEquals(HttpStatus.FORBIDDEN, denied.getStatus());
		assertEquals("Forbidden", denied.getMessage());

		when(acl.canAccess(7L, user)).thenReturn(true);
		assertDoesNotThrow(() -> service.requireAccess(7L));
		assertDoesNotThrow(() -> service.requireAccess(0L));
	}
}
