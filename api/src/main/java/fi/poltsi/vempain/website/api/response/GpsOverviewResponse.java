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
@Schema(name = "GpsOverviewResponse", description = "Overview of a published GPS data set; mirrors the frontend GpsOverviewResponse model")
public class GpsOverviewResponse {
	@Schema(description = "Data set identifier", example = "gps_timeseries_trip")
	private String            identifier;
	@Schema(description = "Number of points with coordinates", example = "1200")
	private long              pointCount;
	@Schema(description = "Bounding box of all points, null when the set is empty")
	private GpsBoundsResponse bounds;
}
