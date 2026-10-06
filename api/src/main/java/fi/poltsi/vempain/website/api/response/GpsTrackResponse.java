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
@Schema(name = "GpsTrackResponse", description = "Ordered GPS track; mirrors the frontend GpsTrackResponse model")
public class GpsTrackResponse {
	@Schema(description = "Data set identifier", example = "gps_timeseries_trip")
	private String                 identifier;
	@Schema(description = "Number of points returned", example = "3000")
	private int                    totalPoints;
	@Schema(description = "Number of points after sampling", example = "3000")
	private int                    sampledPoints;
	@Schema(description = "Sampling step", example = "1")
	private int                    sampleStep;
	@Schema(description = "Track points ordered by timestamp")
	private List<GpsPointResponse> items;
}
