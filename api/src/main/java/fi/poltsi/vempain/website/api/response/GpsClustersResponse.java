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
@Schema(name = "GpsClustersResponse", description = "Clustered view of a GPS data set; mirrors the frontend GpsClustersResponse model")
public class GpsClustersResponse {
	@Schema(description = "Data set identifier", example = "gps_timeseries_trip")
	private String                       identifier;
	@Schema(description = "Effective zoom level", example = "6")
	private int                          zoom;
	@Schema(description = "Clusters")
	private List<GpsClusterItemResponse> items;
	@Schema(description = "Bounding box of the whole data set, null when empty")
	private GpsBoundsResponse            bounds;
}
