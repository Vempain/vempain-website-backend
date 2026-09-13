package fi.poltsi.vempain.website.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Pages", description = "Website page and directory endpoints")
public interface PageApi {
	String BASE_PATH = "/api";

	@GetMapping(path = BASE_PATH + "/pages", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List pages")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Page list returned"),
			@ApiResponse(responseCode = "400", description = "Invalid paging or filtering parameters"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object pages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
	             @RequestParam(defaultValue = "desc") String sort, @RequestParam(defaultValue = "") String search,
	             @RequestParam(required = false) String path);

	@GetMapping(path = BASE_PATH + "/pages/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get a page by ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Page returned"),
			@ApiResponse(responseCode = "403", description = "Page access denied"),
			@ApiResponse(responseCode = "404", description = "Page not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object page(@PathVariable long id);

	@GetMapping(path = BASE_PATH + "/public/pages", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List public pages")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Public page list returned"),
			@ApiResponse(responseCode = "400", description = "Invalid paging or filtering parameters"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object publicPages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
	                   @RequestParam(defaultValue = "desc") String sort, @RequestParam(defaultValue = "") String search,
	                   @RequestParam(required = false) String path);

	@GetMapping(path = BASE_PATH + "/public/pages/{parentId}/children", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List child pages")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Child pages returned"),
			@ApiResponse(responseCode = "404", description = "Parent page not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object children(@PathVariable long parentId);

	@GetMapping(path = BASE_PATH + "/public/page-directories", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List page directories")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Page directories returned"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object directories();

	@GetMapping(path = BASE_PATH + "/public/page-directories/{directory}/tree", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get a page directory tree")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Directory tree returned"),
			@ApiResponse(responseCode = "400", description = "Invalid directory"),
			@ApiResponse(responseCode = "404", description = "Directory not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object tree(@PathVariable String directory);

	@GetMapping(path = BASE_PATH + "/public/page-content", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get page content by file path")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Page content returned"),
			@ApiResponse(responseCode = "400", description = "File path is missing or invalid"),
			@ApiResponse(responseCode = "403", description = "Page access denied"),
			@ApiResponse(responseCode = "404", description = "Page not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object content(@RequestParam("file_path") String path);
}
