package fi.poltsi.vempain.website.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * REST contract for service health checks.
 */
@Tag(name = "Health", description = "Service liveness")
public interface HealthApi {
	/** Base path for health endpoints. */
	String BASE_PATH = "";

	/**
	 * Checks service availability.
	 *
	 * @return health status values
	 */
	@GetMapping(path = BASE_PATH + "/health", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Check service health")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Service is available"),
			@ApiResponse(responseCode = "500", description = "Service health check failed")
	})
	Map<String, String> health();
}
