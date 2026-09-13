package fi.poltsi.vempain.website.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Access to the claims that {@link JwtAuthenticationFilter} attached to the current
 * request. Anonymous callers are represented by an empty optional and by the user
 * identifier {@code -1}, exactly as in the PHP backend.
 */
@Component
public class CurrentUserProvider {

	/**
	 * Request attribute the filter stores the resolved claims in.
	 */
	public static final String REQUEST_ATTRIBUTE = "jwt";

	public Optional<AuthenticatedUser> current() {
		return currentRequest().map(request -> (AuthenticatedUser) request.getAttribute(REQUEST_ATTRIBUTE));
	}

	public long currentUserId() {
		return current().map(AuthenticatedUser::userId)
		                .orElse(AuthenticatedUser.ANONYMOUS_USER_ID);
	}

	public boolean hasGlobalPermission() {
		return current().map(AuthenticatedUser::globalPermission)
		                .orElse(false);
	}

	private Optional<HttpServletRequest> currentRequest() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return Optional.of(attributes.getRequest());
		}

		return Optional.empty();
	}
}
