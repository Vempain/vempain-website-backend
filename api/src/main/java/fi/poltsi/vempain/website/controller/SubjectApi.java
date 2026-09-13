package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.controller.dto.response.SubjectResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "Subjects", description = "Subject autocomplete and word-cloud data")
public interface SubjectApi {
	String BASE_PATH = "/api/public";

	@GetMapping(path = BASE_PATH + "/subjects/autocomplete", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Autocomplete subjects")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Matching subjects returned"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	List<SubjectResponse> autocomplete(@RequestParam(defaultValue = "") String q);

	@GetMapping(path = BASE_PATH + "/embeds/word-cloud", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get subject word cloud")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Word-cloud terms returned"),
			@ApiResponse(responseCode = "400", description = "Invalid result count"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	List<Map<String, Object>> wordCloud(@RequestParam(defaultValue = "50") int count);
}
