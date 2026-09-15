package fi.poltsi.vempain.website.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Keeps browser-facing hardening independent of the optional reverse proxy.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
	                                FilterChain filterChain) throws ServletException, IOException {
		response.setHeader("Content-Security-Policy",
		                   "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'");
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
		response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
		if (request.isSecure()) {
			response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		}
		filterChain.doFilter(request, response);
	}
}
