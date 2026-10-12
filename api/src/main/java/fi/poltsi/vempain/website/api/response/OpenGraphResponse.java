package fi.poltsi.vempain.website.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "OpenGraphResponse",
		description = "Open Graph metadata of a website route; mirrors the frontend OpenGraph model. The frontend turns path and image_path into "
					  + "absolute URLs with the origin the site is served from")
public class OpenGraphResponse {
	@Schema(description = "Open Graph object type: article for a published page, website for everything else", example = "article")
	private String        type;
	@Schema(description = "Site name from the site configuration", example = "My Web Site")
	private String        siteName;
	@Schema(description = "Title of the route", example = "Summer trip")
	private String        title;
	@Schema(description = "Short description of the route, null when there is none", example = "Photos from the summer trip")
	private String        description;
	@Schema(description = "Canonical frontend path of the route", example = "/pages/trips/summer")
	private String        path;
	@Schema(description = "Site relative path of the preview image, served below /file; null when the route has no image", example = "trips/summer/beach.jpg")
	private String        imagePath;
	@Schema(description = "MIME type of the preview image", example = "image/jpeg")
	private String        imageMimetype;
	@Schema(description = "Width of the preview image in pixels", example = "1400")
	private Long          imageWidth;
	@Schema(description = "Height of the preview image in pixels", example = "933")
	private Long          imageHeight;
	@Schema(description = "When the page or gallery was published or created", example = "2024-05-15T12:00:00")
	private LocalDateTime published;
	@Schema(description = "When the page or gallery was last modified", example = "2024-05-16T09:30:00")
	private LocalDateTime modified;
}
