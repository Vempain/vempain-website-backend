package fi.poltsi.vempain.website.controller;

import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Liveness endpoint used by the container health check and by Traefik.
 */
@RestController
public class HealthController implements HealthApi {

	public Map<String, String> health() {
		return Map.of("status", "ok");
	}
}
