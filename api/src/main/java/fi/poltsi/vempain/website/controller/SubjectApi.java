package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.controller.dto.response.SubjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * REST contract for subject-related endpoints.
 */
@Tag(name = "Subjects", description = "Subject autocomplete and word-cloud data")
public interface SubjectApi {
	/** Base path for public endpoints. */
	String BASE_PATH = "/api/public";

	/**
	 * Finds subjects matching a query.
	 *
	 * @param q search query
	 * @return matching subjects
	 */
	@GetMapping(path = BASE_PATH + "/subjects/autocomplete", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Autocomplete subjects")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Matching subjects returned"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	List<SubjectResponse> autocomplete(@RequestParam(defaultValue = "") String q);

	/**
	 * Returns subject frequency data for a word cloud.
	 *
	 * @param count maximum number of terms
	 * @return word-cloud terms and counts
	 */
	@GetMapping(path = BASE_PATH + "/embeds/word-cloud", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get subject word cloud")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Word-cloud terms returned"),
			@ApiResponse(responseCode = "400", description = "Invalid result count"),
			@ApiResponse(responseCode = "500", description = "Unexpected server error")
	})
	List<Map<String, Object>> wordCloud(@RequestParam(defaultValue = "50") int count);
}
