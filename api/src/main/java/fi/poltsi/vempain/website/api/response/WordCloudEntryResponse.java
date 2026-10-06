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
@Schema(name = "WordCloudEntryResponse", description = "Subject usage count for the word cloud embed; mirrors the renderer WordCloudEmbedDataItem model")
public class WordCloudEntryResponse {
	@Schema(description = "Lower cased subject text", example = "travel")
	private String text;
	@Schema(description = "Number of pages, files and galleries using the subject", example = "17")
	private Long   value;
}
