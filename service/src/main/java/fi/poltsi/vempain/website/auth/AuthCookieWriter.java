package fi.poltsi.vempain.website.auth;

import fi.poltsi.vempain.website.config.SiteProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes and clears the HttpOnly authentication cookie. The cookie is built by hand
 * because {@code SameSite} is not exposed by the servlet cookie API.
 */
@Component
public class AuthCookieWriter {

	private static final String AUTH_TOKEN_HEADER = "X-Auth-Token";

	private final SiteProperties siteProperties;

	public AuthCookieWriter(SiteProperties siteProperties) {
		this.siteProperties = siteProperties;
	}

	/**
	 * Reads the token from the {@code Authorization} header, falling back to the cookie.
	 */
	public String readToken(HttpServletRequest request) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
			String token = authorization.substring(7)
			                            .trim();
			if (!token.isEmpty()) {
				return token;
			}
		}

		if (request.getCookies() != null) {
			for (var cookie : request.getCookies()) {
				if (siteProperties.getJwtCookieName()
				                  .equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}

		return null;
	}

	public void write(HttpServletResponse response, String token, long maxAgeSeconds) {
		response.setHeader(AUTH_TOKEN_HEADER, token);
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(token, maxAgeSeconds));
	}

	public void clear(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0L));
	}

	private String cookie(String value, long maxAgeSeconds) {
		List<String> parts = new ArrayList<>();
		parts.add(siteProperties.getJwtCookieName() + "=" + value);
		parts.add("Path=/");
		parts.add("HttpOnly");
		parts.add("SameSite=Lax");
		parts.add("Max-Age=" + maxAgeSeconds);
		if (siteProperties.isJwtCookieSecure()) {
			parts.add("Secure");
		}

		return String.join("; ", parts);
	}
}
