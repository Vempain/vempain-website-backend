package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.StatusResponse;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness endpoint used by the container health check and by Traefik.
 */
@RestController
public class HealthController implements HealthApi {

	public StatusResponse health() {
		return StatusResponse.builder()
							 .status("ok")
							 .build();
	}
}
