package fi.poltsi.vempain.website.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "Health", description = "Service liveness")
public interface HealthApi {
	String BASE_PATH = "";

	@GetMapping(path = BASE_PATH + "/health", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Check service health")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Service is available"),
			@ApiResponse(responseCode = "500", description = "Service health check failed")
	})
	Map<String, String> health();
}
