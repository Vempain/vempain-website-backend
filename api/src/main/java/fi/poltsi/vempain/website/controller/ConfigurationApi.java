package fi.poltsi.vempain.website.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "Configuration", description = "Public website configuration")
public interface ConfigurationApi {
	String BASE_PATH = "/api/public";

	@GetMapping(path = BASE_PATH + "/configuration", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get public configuration", description = "Return the effective public configuration values")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Effective public configuration"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Map<String, String> configuration();
}
