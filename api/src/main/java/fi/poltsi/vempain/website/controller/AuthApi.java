package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.controller.dto.request.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Tag(name = "Authentication", description = "Authentication and session management")
public interface AuthApi {
	String BASE_PATH = "/api";

	@PostMapping(path = BASE_PATH + "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Log in", description = "Validate credentials and issue an authentication token")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Credentials accepted and token issued"),
			@ApiResponse(responseCode = "400", description = "Username or password is missing"),
			@ApiResponse(responseCode = "401", description = "Invalid credentials"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Map<String, String> login(@RequestBody(required = false) LoginRequest request, HttpServletResponse response);

	@PostMapping(path = BASE_PATH + "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Log out", description = "Invalidate the current authentication token and clear the session cookie")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Session cleared"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Map<String, String> logout(HttpServletResponse response);
}
