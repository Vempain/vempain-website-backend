package fi.poltsi.vempain.website.auth;

import fi.poltsi.vempain.website.exception.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Resolves the caller from the JWT. Handling is deliberately permissive: a missing or
 * malformed token simply leaves the request anonymous, because most of the site is public.
 * An expired token, on the other hand, ends the request with a 401 and clears the cookie
 * so that the frontend can drop its mirrored copy of the token.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService       jwtService;
	private final AuthCookieWriter cookieWriter;

	public JwtAuthenticationFilter(JwtService jwtService, AuthCookieWriter cookieWriter) {
		this.jwtService   = jwtService;
		this.cookieWriter = cookieWriter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain filterChain) throws ServletException, IOException {
		String                      token = cookieWriter.readToken(request);
		Optional<AuthenticatedUser> user;

		try {
			user = jwtService.verify(token);
		} catch (TokenExpiredException expired) {
			cookieWriter.clear(response);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter()
			        .write("{\"error\":\"Session expired\",\"code\":\"SESSION_EXPIRED\"}");
			return;
		}

		user.filter(authenticated -> jwtService.isPersistedAndValid(authenticated.token()))
		    .ifPresent(authenticated ->
							   request.setAttribute(CurrentUserProvider.REQUEST_ATTRIBUTE, authenticated));

		filterChain.doFilter(request, response);

		if (user.filter(authenticated -> jwtService.isPersistedAndValid(authenticated.token()))
		        .isPresent()
		    && !response.isCommitted()) {
			cookieWriter.write(response, jwtService.refresh(user.orElseThrow()), jwtService.ttlSeconds());
		}
	}
}
