package fi.poltsi.vempain.website.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserProviderUTC {

    private final CurrentUserProvider provider = new CurrentUserProvider();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void claimsAttachedByTheFilterAreExposed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUserProvider.REQUEST_ATTRIBUTE, new AuthenticatedUser(42L, "alice", true, "token"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("alice", provider.current()
            .orElseThrow()
            .username());
        assertEquals(42L, provider.currentUserId());
        assertTrue(provider.hasGlobalPermission());
    }

    @Test
    void requestWithoutClaimsIsAnonymous() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        assertTrue(provider.current()
            .isEmpty());
        assertEquals(AuthenticatedUser.ANONYMOUS_USER_ID, provider.currentUserId());
        assertFalse(provider.hasGlobalPermission());
    }

    @Test
    void noRequestContextIsAnonymous() {
        RequestContextHolder.resetRequestAttributes();

        assertTrue(provider.current()
            .isEmpty());
        assertEquals(-1L, provider.currentUserId());
        assertFalse(provider.hasGlobalPermission());
    }
}
