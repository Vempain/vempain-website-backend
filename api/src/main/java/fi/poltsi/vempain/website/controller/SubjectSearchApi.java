package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.request.SubjectIdSearchRequest;
import fi.poltsi.vempain.website.api.request.SubjectSearchRequest;
import fi.poltsi.vempain.website.api.response.ApiErrorResponse;
import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.SubjectSearchResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * REST contract for subject-based searches.
 */
@Tag(name = "Subject search", description = "Search endpoints used by public subject controls")
public interface SubjectSearchApi {
	/** Base path for public endpoints. */
	String BASE_PATH = "/api/public";

	/**
	 * Searches pages by free text.
	 *
	 * @param request search criteria, every field is optional
	 * @return matching page summaries
	 */
	@PostMapping(path = BASE_PATH + "/subject-search", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Search pages by text", description = "Returns a page of page summaries (PagedResponse of WebSitePageResponse)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Search results returned",
						 content = @Content(schema = @Schema(implementation = PagedResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Malformed search request",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	PagedResponse<WebSitePageResponse> search(@RequestBody(required = false) SubjectSearchRequest request);

	/**
	 * Searches pages, galleries and files linked to the given subjects.
	 *
	 * @param request search criteria, every field is optional
	 * @return matching pages, galleries and files
	 */
	@PostMapping(path = BASE_PATH + "/subjects/search", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Search subject-linked resources")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Subject search results returned",
						 content = @Content(schema = @Schema(implementation = SubjectSearchResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Malformed search request",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	SubjectSearchResponse searchIds(@RequestBody(required = false) SubjectIdSearchRequest request);
}
