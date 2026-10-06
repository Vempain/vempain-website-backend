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
@Schema(name = "GpsClusterPointsResponse", description = "Points inside one cluster cell; mirrors the frontend GpsClusterPointsResponse model")
public class GpsClusterPointsResponse {
	@Schema(description = "Data set identifier", example = "gps_timeseries_trip")
	private String                 identifier;
	@Schema(description = "Cluster key", example = "6:42:118")
	private String                 clusterKey;
	@Schema(description = "Bounding box of the cluster cell")
	private GpsBoundsResponse      bounds;
	@Schema(description = "Points inside the cell")
	private List<GpsPointResponse> items;
}
