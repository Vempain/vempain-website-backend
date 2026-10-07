package fi.poltsi.vempain.website.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "SubjectSearchRequest", description = "Free text page search used by the public subject search control")
public class SubjectSearchRequest {
	@Schema(description = "Zero-based page number, defaults to 0", example = "0")
	@Min(0)
	private Integer page;
	@Schema(description = "Page size, defaults to 12 and is capped at 50", example = "12")
	@Min(1)
	@Max(50)
	private Integer size;
	@Schema(description = "Requested sort field (currently ignored)", example = "id")
	@Size(max = 50)
	private String  sortBy;
	@Schema(description = "Requested sort direction (currently ignored)", example = "ASC")
	@Size(max = 4)
	private String  direction;
	@Schema(description = "Search text", example = "sunset")
	@Size(max = 200)
	private String  search;
	@Schema(description = "Whether the search is case sensitive (currently ignored)", example = "false")
	private Boolean caseSensitive;
}
