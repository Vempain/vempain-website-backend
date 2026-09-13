package fi.poltsi.vempain.website.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Website", description = "Public website page rendering")
public interface WebsiteApi {
	String BASE_PATH = "";

	@GetMapping(path = {BASE_PATH + "/", BASE_PATH + "/{path:.+}"}, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Render a website page")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Website page returned"),
			@ApiResponse(responseCode = "403", description = "Page access denied"),
			@ApiResponse(responseCode = "404", description = "Page not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object page(@PathVariable(required = false) String path);
}
