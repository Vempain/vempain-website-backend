package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.ApiErrorResponse;
import fi.poltsi.vempain.website.api.response.SubjectResponse;
import fi.poltsi.vempain.website.api.response.WordCloudEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
			@ApiResponse(responseCode = "200", description = "Matching subjects returned",
						 content = @Content(array = @ArraySchema(schema = @Schema(implementation =
								 SubjectResponse.class)), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	List<SubjectResponse> autocomplete(@RequestParam(defaultValue = "") @Size(max = 200) String q);

	/**
	 * Returns subject frequency data for a word cloud.
	 *
	 * @param count maximum number of terms
	 * @return word-cloud terms and counts
	 */
	@GetMapping(path = BASE_PATH + "/embeds/word-cloud", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get subject word cloud")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Word-cloud terms returned",
						 content = @Content(array = @ArraySchema(schema = @Schema(implementation =
								 WordCloudEntryResponse.class)), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Invalid result count",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "500", description = "Unexpected server error",
						 content = @Content(schema = @Schema(implementation = ApiErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
	})
	List<WordCloudEntryResponse> wordCloud(@RequestParam(defaultValue = "50") int count);
}
