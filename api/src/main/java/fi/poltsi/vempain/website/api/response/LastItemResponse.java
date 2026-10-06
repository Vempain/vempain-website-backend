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
@Schema(name = "LastItemResponse", description = "Recently published item; mirrors the frontend LastItemsResponseItem model")
public class LastItemResponse {
	@Schema(description = "Site database identifier", example = "7")
	private Long          id;
	@Schema(description = "Item title", example = "Summer trip")
	private String        title;
	@Schema(description = "Publish time (original date time for files), null for galleries", example = "2024-05-15T12:00:00")
	private LocalDateTime published;
	@Schema(description = "Site relative path", example = "trips/summer")
	private String        filePath;
	@Schema(description = "Thumbnail path for files", example = "images/.thumb/a.jpg")
	private String        thumbnailPath;
	@Schema(description = "Gallery identifier for galleries", example = "1050")
	private Long          galleryId;
	@Schema(description = "Page header for pages", example = "Summer trip 2024")
	private String        header;
	@Schema(description = "Rendered page body for pages", example = "<p>Hello</p>")
	private String        body;
}
