package fi.poltsi.vempain.website.controller;

import io.swagger.v3.oas.annotations.Operation;
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
	 * Lists files.
	 *
	 * @param page zero-based page number
	 * @param perPage requested page size
	 * @return file list
	 */
	@GetMapping(path = {BASE_PATH + "/files", PUBLIC_PATH + "/files"}, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List files")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "File list returned"),
			@ApiResponse(responseCode = "400", description = "Invalid paging parameters"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object listFiles(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int perPage);

	/**
	 * Returns public file metadata by identifier.
	 *
	 * @param id file identifier
	 * @return file metadata
	 */
	@GetMapping(path = PUBLIC_PATH + "/files/id/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get a public file by ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "File metadata returned"),
			@ApiResponse(responseCode = "403", description = "File access denied"),
			@ApiResponse(responseCode = "404", description = "File not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object fileById(@PathVariable long id);

	/**
	 * Lists galleries.
	 *
	 * @return galleries
	 */
	@GetMapping(path = BASE_PATH + "/galleries", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List galleries")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Gallery list returned"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object galleries();

	/**
	 * Lists public galleries.
	 *
	 * @return public galleries
	 */
	@GetMapping(path = PUBLIC_PATH + "/galleries", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List public galleries")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Public gallery list returned"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object publicGalleries();

	/**
	 * Lists files in a gallery.
	 *
	 * @param galleryId gallery identifier
	 * @param page zero-based page number
	 * @param perPage requested page size
	 * @return gallery files
	 */
	@GetMapping(path = {BASE_PATH + "/galleries/{galleryId}/files", PUBLIC_PATH + "/galleries/{galleryId}/files"},
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List files in a gallery")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Gallery files returned"),
			@ApiResponse(responseCode = "400", description = "Invalid gallery or paging parameters"),
			@ApiResponse(responseCode = "403", description = "Gallery access denied"),
			@ApiResponse(responseCode = "404", description = "Gallery not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object galleryFiles(@PathVariable long galleryId, @RequestParam(defaultValue = "0") int page,
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
			@ApiResponse(responseCode = "403", description = "File metadata is not visible"),
			@ApiResponse(responseCode = "404", description = "File content not found"),
			@ApiResponse(responseCode = "416", description = "Requested range is not satisfiable"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	ResponseEntity<Resource> raw(@PathVariable String path, HttpServletRequest request) throws java.io.IOException;
}
