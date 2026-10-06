package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.request.LoginRequest;
import fi.poltsi.vempain.website.api.response.LoginResponse;
import fi.poltsi.vempain.website.api.response.StatusResponse;
import fi.poltsi.vempain.website.auth.AuthCookieWriter;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.auth.JwtService;
import fi.poltsi.vempain.website.auth.PasswordVerifier;
import fi.poltsi.vempain.website.entity.WebSiteUser;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSiteUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

	private final WebSiteUserRepository userRepository;
	private final PasswordVerifier      passwordVerifier;
	private final JwtService            jwtService;
	private final AuthCookieWriter      cookieWriter;
	private final CurrentUserProvider   currentUserProvider;

	public LoginResponse login(@RequestBody(required = false) LoginRequest request, HttpServletResponse response) {
		if (request == null || request.getUsername() == null || request.getPassword() == null) {
			throw ApiException.badRequest("Username and password are required");
		}

		WebSiteUser user = userRepository.findByUsername(request.getUsername())
										 .filter(candidate -> passwordVerifier.matches(request.getPassword(), candidate.getPasswordHash()))
		                                 .orElseThrow(() -> {
											 log.info("Rejected a login attempt for user {}", request.getUsername());
											 return ApiException.unauthorized("Invalid credentials");
										 });

		String token = jwtService.issueToken(user.getId(), user.getUsername(), user.isGlobalPermission());
		cookieWriter.write(response, token, jwtService.ttlSeconds());

		return LoginResponse.builder()
							.token(token)
							.build();
	}

	public StatusResponse logout(HttpServletResponse response) {
		currentUserProvider.current()
		                   .ifPresent(user -> jwtService.revoke(user.token()));
		cookieWriter.clear(response);

		return StatusResponse.builder()
							 .status("ok")
							 .build();
	}
}
