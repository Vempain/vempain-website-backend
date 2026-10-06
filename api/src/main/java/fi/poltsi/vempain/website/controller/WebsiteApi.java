package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.ApiErrorResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * REST contract for public website page rendering.
 */
@Tag(name = "Website", description = "Public website page rendering")
public interface WebsiteApi {
	/** Base path for website endpoints. */
	String BASE_PATH = "";

	/**
	 * Renders a website page.
	 *
	 * @param path optional website page path
	 * @return rendered page data
	 */
	@GetMapping(path = {BASE_PATH + "/", BASE_PATH + "/{path:.+}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Render a website page")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Website page returned",
						 content = @Content(schema = @Schema(implementation = WebSitePageResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Authentication required",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "403", description = "Page access denied",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "404", description = "Page not found",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	WebSitePageResponse page(@PathVariable(required = false) String path);
}
