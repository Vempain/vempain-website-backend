package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.request.LoginRequest;
import fi.poltsi.vempain.website.api.response.ApiErrorResponse;
import fi.poltsi.vempain.website.api.response.LoginResponse;
import fi.poltsi.vempain.website.api.response.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * REST contract for authentication and session management.
 */
@Tag(name = "Authentication", description = "Authentication and session management")
public interface AuthApi {
	/**
	 * Base path for authentication endpoints.
	 */
	String BASE_PATH = "/api";

	/**
	 * Authenticates a user and issues an authentication token.
	 *
	 * @param request  credentials to validate
	 * @param response response used to establish the authenticated session
	 * @return issued token
	 */
	@PostMapping(path = BASE_PATH + "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Log in", description = "Validate credentials and issue an authentication token")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Credentials accepted and token issued",
						 content = @Content(schema = @Schema(implementation = LoginResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Username or password is missing",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Invalid credentials",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	LoginResponse login(@RequestBody(required = false) LoginRequest request, HttpServletResponse response);

	/**
	 * Invalidates the current authenticated session.
	 *
	 * @param response response used to clear the session cookie
	 * @return status acknowledgement
	 */
	@PostMapping(path = BASE_PATH + "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Log out", description = "Invalidate the current authentication token and clear the session cookie")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Session cleared",
						 content = @Content(schema = @Schema(implementation = StatusResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	StatusResponse logout(HttpServletResponse response);
}
