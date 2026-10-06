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
@Schema(name = "GpsBoundsResponse", description = "Bounding box in decimal degrees; mirrors the frontend GpsBounds model")
public class GpsBoundsResponse {
	@Schema(description = "Minimum latitude", example = "60.1")
	private Double minLatitude;
	@Schema(description = "Maximum latitude", example = "60.3")
	private Double maxLatitude;
	@Schema(description = "Minimum longitude", example = "24.8")
	private Double minLongitude;
	@Schema(description = "Maximum longitude", example = "25.1")
	private Double maxLongitude;
}
