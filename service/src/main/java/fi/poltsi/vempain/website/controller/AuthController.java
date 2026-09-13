package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.auth.AuthCookieWriter;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.auth.JwtService;
import fi.poltsi.vempain.website.auth.PasswordVerifier;
import fi.poltsi.vempain.website.entity.WebSiteUser;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSiteUserRepository;
import fi.poltsi.vempain.website.controller.dto.request.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController implements AuthApi {

	private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

	private final WebSiteUserRepository userRepository;
	private final PasswordVerifier      passwordVerifier;
	private final JwtService            jwtService;
	private final AuthCookieWriter      cookieWriter;
	private final CurrentUserProvider   currentUserProvider;

	public AuthController(WebSiteUserRepository userRepository,
	                      PasswordVerifier passwordVerifier,
	                      JwtService jwtService,
	                      AuthCookieWriter cookieWriter,
	                      CurrentUserProvider currentUserProvider) {
		this.userRepository      = userRepository;
		this.passwordVerifier    = passwordVerifier;
		this.jwtService          = jwtService;
		this.cookieWriter        = cookieWriter;
		this.currentUserProvider = currentUserProvider;
	}

	public Map<String, String> login(@RequestBody(required = false) LoginRequest request, HttpServletResponse response) {
		if (request == null || request.username() == null || request.password() == null) {
			throw ApiException.badRequest("Username and password are required");
		}

		WebSiteUser user = userRepository.findByUsername(request.username())
		                                 .filter(candidate -> passwordVerifier.matches(request.password(), candidate.getPasswordHash()))
		                                 .orElseThrow(() -> {
											 LOG.info("Rejected a login attempt for user {}", request.username());
											 return ApiException.unauthorized("Invalid credentials");
										 });

		String token = jwtService.issueToken(user.getId(), user.getUsername(), user.isGlobalPermission());
		cookieWriter.write(response, token, jwtService.ttlSeconds());

		return Map.of("token", token);
	}

	public Map<String, String> logout(HttpServletResponse response) {
		currentUserProvider.current()
		                   .ifPresent(user -> jwtService.revoke(user.token()));
		cookieWriter.clear(response);

		return Map.of("status", "ok");
	}

}
