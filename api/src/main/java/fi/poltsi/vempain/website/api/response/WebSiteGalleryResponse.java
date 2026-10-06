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
@Schema(name = "WebSiteGalleryResponse", description = "Published gallery; mirrors the frontend WebSiteGallery model")
public class WebSiteGalleryResponse {
	@Schema(description = "Site database identifier of the gallery", example = "3")
	private Long                  id;
	@Schema(description = "Identifier of the gallery in the admin backend", example = "1050")
	private Long                  galleryId;
	@Schema(description = "Gallery short name", example = "Summer")
	private String                shortname;
	@Schema(description = "Gallery description", example = "Summer trip photos")
	private String                description;
	@Schema(description = "ACL identifier, null for public galleries", example = "5")
	private Long                  aclId;
	@Schema(description = "Subjects attached to the gallery")
	private List<SubjectResponse> subjects;
}
