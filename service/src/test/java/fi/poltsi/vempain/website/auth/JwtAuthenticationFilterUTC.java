package fi.poltsi.vempain.website.auth;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterUTC {

	@Test
	void revokedTokenIsAnonymousAndIsNotRefreshed() throws Exception {
		JwtService       jwt     = mock(JwtService.class);
		AuthCookieWriter cookies = mock(AuthCookieWriter.class);
		when(cookies.readToken(any())).thenReturn("revoked");
		when(jwt.verify("revoked")).thenReturn(Optional.of(new AuthenticatedUser(7, "alice", true, "revoked")));
		when(jwt.isPersistedAndValid("revoked")).thenReturn(false);

		MockHttpServletRequest  request  = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain             chain    = new MockFilterChain();
		new JwtAuthenticationFilter(jwt, cookies).doFilter(request, response, chain);

		assertNull(request.getAttribute(CurrentUserProvider.REQUEST_ATTRIBUTE));
		verify(cookies, never()).write(any(), anyString(), anyLong());
	}

	@Test
	void activeTokenIsAttachedAndRefreshed() throws Exception {
		JwtService        jwt     = mock(JwtService.class);
		AuthCookieWriter  cookies = mock(AuthCookieWriter.class);
		AuthenticatedUser user    = new AuthenticatedUser(7, "alice", false, "active");
		when(cookies.readToken(any())).thenReturn("active");
		when(jwt.verify("active")).thenReturn(Optional.of(user));
		when(jwt.isPersistedAndValid("active")).thenReturn(true);
		when(jwt.refresh(user)).thenReturn("refreshed");
		when(jwt.ttlSeconds()).thenReturn(1200L);

		MockHttpServletRequest  request  = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		new JwtAuthenticationFilter(jwt, cookies).doFilter(request, response, new MockFilterChain());

		verify(cookies).write(response, "refreshed", 1200L);
	}
}
