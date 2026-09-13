package fi.poltsi.vempain.website.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Embeds", description = "Public embedded music, GPS, and recent-item data")
public interface EmbedApi {
	String BASE_PATH = "/api/public/embeds";

	@GetMapping(path = BASE_PATH + "/music/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get music data")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Music data returned"),
			@ApiResponse(responseCode = "400", description = "Invalid dataset or request parameters"),
			@ApiResponse(responseCode = "404", description = "Published dataset not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object music(@PathVariable String id, @RequestParam(defaultValue = "0") int page,
	             @RequestParam(defaultValue = "25") int perPage, @RequestParam(defaultValue = "artist") String sortBy,
	             @RequestParam(defaultValue = "asc") String direction, @RequestParam(defaultValue = "") String search);

	@GetMapping(path = BASE_PATH + "/gps/{id}/overview", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get GPS overview")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "GPS overview returned"),
			@ApiResponse(responseCode = "400", description = "Invalid dataset identifier"),
			@ApiResponse(responseCode = "404", description = "Published dataset not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object overview(@PathVariable String id);

	@GetMapping(path = BASE_PATH + "/gps/{id}/track", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get GPS track")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "GPS track returned"),
			@ApiResponse(responseCode = "400", description = "Invalid dataset or request parameters"),
			@ApiResponse(responseCode = "404", description = "Published dataset not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object track(@PathVariable String id, @RequestParam(defaultValue = "3000") int maxPoints);

	@GetMapping(path = BASE_PATH + "/gps/{id}/clusters", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get GPS clusters")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "GPS clusters returned"),
			@ApiResponse(responseCode = "400", description = "Invalid dataset or request parameters"),
			@ApiResponse(responseCode = "404", description = "Published dataset not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object clusters(@PathVariable String id, @RequestParam(defaultValue = "4") int zoom,
	                @RequestParam(required = false) Double minLat, @RequestParam(required = false) Double maxLat,
	                @RequestParam(required = false) Double minLng, @RequestParam(required = false) Double maxLng);

	@GetMapping(path = BASE_PATH + "/gps/{id}/clusters/{key}/points", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get points in a GPS cluster")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Cluster points returned"),
			@ApiResponse(responseCode = "400", description = "Invalid cluster key or request parameters"),
			@ApiResponse(responseCode = "404", description = "Published dataset not found"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object points(@PathVariable String id, @PathVariable String key, @RequestParam(defaultValue = "250") int limit);

	@GetMapping(path = BASE_PATH + "/last", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get latest published items")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Latest items returned"),
			@ApiResponse(responseCode = "400", description = "Unsupported item type or invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	Object last(@RequestParam(defaultValue = "") String type, @RequestParam(defaultValue = "5") int count);
}
