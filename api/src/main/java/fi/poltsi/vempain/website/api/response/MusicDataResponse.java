package fi.poltsi.vempain.website.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "MusicDataResponse", description = "Paged music data set; mirrors the frontend MusicDataResponse model")
public class MusicDataResponse {
	@Schema(description = "Data set identifier", example = "music_collection")
	private String                     identifier;
	@Schema(description = "Rows on the current page")
	private List<MusicDataRowResponse> items;
	@Schema(description = "Zero-based page number", example = "0")
	private int                        page;
	@Schema(description = "Page size", example = "25")
	private int                        size;
	@Schema(description = "Total number of rows", example = "100")
	private long                       totalElements;
	@Schema(description = "Total number of pages", example = "4")
	private int                        totalPages;
	@Schema(description = "Whether this is the first page", example = "true")
	private boolean                    first;
	@Schema(description = "Whether this is the last page", example = "false")
	private boolean                    last;
	@Schema(description = "Effective sort column", example = "artist")
	private String                     sortBy;
	@Schema(description = "Effective sort direction (asc or desc)", example = "asc")
	private String                     direction;
	@Schema(description = "Effective search text", example = "rock")
	private String                     search;
}
