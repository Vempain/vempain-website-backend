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
@Schema(name = "LastItemsResponse", description = "Recently published items; mirrors the frontend LastItemsResponse model")
public class LastItemsResponse {
	@Schema(description = "Requested item type", example = "images")
	private String                 type;
	@Schema(description = "Effective item count", example = "5")
	private int                    count;
	@Schema(description = "Items, newest first")
	private List<LastItemResponse> items;
}
