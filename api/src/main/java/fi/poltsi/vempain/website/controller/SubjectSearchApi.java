package fi.poltsi.vempain.website.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "Subject search", description = "Search endpoints used by public subject controls")
public interface SubjectSearchApi {
	String BASE_PATH = "/api/public";

	@PostMapping(path = BASE_PATH + "/subject-search", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Search pages by subject text")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Search results returned"),
			@ApiResponse(responseCode = "400", description = "Invalid search request"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object search(@RequestBody(required = false) Map<String, Object> body);

	@PostMapping(path = BASE_PATH + "/subjects/search", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Search subject-linked pages")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Subject search results returned"),
			@ApiResponse(responseCode = "400", description = "Invalid search request"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object searchIds(@RequestBody(required = false) Map<String, Object> body);
}
