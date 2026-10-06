package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.ApiErrorResponse;
import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.WebSiteFileResponse;
import fi.poltsi.vempain.website.api.response.WebSiteGalleryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * REST contract for files and galleries.
 */
@Tag(name = "Resources", description = "Files and galleries")
public interface ResourceApi {
	/** Base path for resource endpoints. */
	String BASE_PATH = "/api";
	/** Base path for public resource endpoints. */
	String PUBLIC_PATH = BASE_PATH + "/public";

	/**
	 * Lists files visible to the caller.
	 *
	 * @param page zero-based page number
	 * @param perPage requested page size
	 * @return file list
	 */
	@GetMapping(path = {BASE_PATH + "/files", PUBLIC_PATH + "/files"}, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List files", description = "Returns a page of file metadata (PagedResponse of WebSiteFileResponse)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "File list returned",
						 content = @Content(schema = @Schema(implementation = PagedResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Invalid paging parameters",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	PagedResponse<WebSiteFileResponse> listFiles(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int perPage);

	/**
	 * Returns public file metadata by the file backend identifier.
	 *
	 * @param id file identifier
	 * @return file metadata
	 */
	@GetMapping(path = PUBLIC_PATH + "/files/id/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get a public file by ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "File metadata returned",
						 content = @Content(schema = @Schema(implementation = WebSiteFileResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Authentication required",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "403", description = "File access denied",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "404", description = "File not found",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	WebSiteFileResponse fileById(@PathVariable long id);

	/**
	 * Lists all galleries.
	 *
	 * @return galleries
	 */
	@GetMapping(path = BASE_PATH + "/galleries", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List galleries")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Gallery list returned",
						 content = @Content(array = @ArraySchema(schema = @Schema(implementation =
								 WebSiteGalleryResponse.class)), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	List<WebSiteGalleryResponse> galleries();

	/**
	 * Lists galleries visible to the caller.
	 *
	 * @return public galleries
	 */
	@GetMapping(path = PUBLIC_PATH + "/galleries", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List public galleries")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Public gallery list returned",
						 content = @Content(array = @ArraySchema(schema = @Schema(implementation =
								 WebSiteGalleryResponse.class)), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	List<WebSiteGalleryResponse> publicGalleries();

	/**
	 * Lists files in a gallery.
	 *
	 * @param galleryId gallery identifier (admin backend gallery ID)
	 * @param page zero-based page number
	 * @param perPage requested page size
	 * @return gallery files
	 */
	@GetMapping(path = {BASE_PATH + "/galleries/{galleryId}/files", PUBLIC_PATH + "/galleries/{galleryId}/files"},
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List files in a gallery", description = "Returns a page of file metadata (PagedResponse of WebSiteFileResponse)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Gallery files returned",
						 content = @Content(schema = @Schema(implementation = PagedResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Invalid gallery or paging parameters",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "403", description = "Gallery access denied",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "404", description = "Gallery not found",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	PagedResponse<WebSiteFileResponse> galleryFiles(@PathVariable long galleryId, @RequestParam(defaultValue = "0") int page,
													@RequestParam(defaultValue = "25") int perPage);

	/**
	 * Streams file content.
	 *
	 * @param path file path
	 * @param request incoming HTTP request
	 * @return file content response
	 * @throws java.io.IOException if file content cannot be read
	 */
	@GetMapping(path = {"/file/{*path}", PUBLIC_PATH + "/files/{*path}"},
			produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@Operation(summary = "Stream a file")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "File content returned"),
			@ApiResponse(responseCode = "206", description = "Partial file content returned for a valid range"),
			@ApiResponse(responseCode = "403", description = "File metadata is not visible", content = @Content),
			@ApiResponse(responseCode = "404", description = "File content not found", content = @Content),
			@ApiResponse(responseCode = "416", description = "Requested range is not satisfiable", content = @Content),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	ResponseEntity<Resource> raw(@PathVariable String path, HttpServletRequest request) throws java.io.IOException;
}
