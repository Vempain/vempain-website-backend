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
@Schema(name = "EmbedItemResponse", description = "Inline item of a collapse or carousel embed; mirrors the frontend EmbedItem model")
public class EmbedItemResponse {
	@Schema(description = "Item title", example = "Chapter 1")
	private String title;
	@Schema(description = "Item body HTML", example = "<p>Text</p>")
	private String body;
}
