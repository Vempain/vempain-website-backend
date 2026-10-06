package fi.poltsi.vempain.website.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "SubjectSearchResponse", description = "Subject search results grouped by resource type, each with its own pagination")
public class SubjectSearchResponse {
	@Schema(description = "Matching pages")
	private PagedResponse<WebSitePageResponse>    pages;
	@Schema(description = "Matching galleries")
	private PagedResponse<WebSiteGalleryResponse> galleries;
	@Schema(description = "Matching files")
	private PagedResponse<WebSiteFileResponse>    files;
}
