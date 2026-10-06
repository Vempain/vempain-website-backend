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
@Schema(name = "GpsClusterItemResponse", description = "Cluster or single point of a GPS data set at a zoom level; mirrors the frontend GpsClusterItem model")
public class GpsClusterItemResponse {
	@Schema(description = "Cluster key in the form zoom:latBucket:lngBucket", example = "6:42:118")
	private String            clusterKey;
	@Schema(description = "cluster when it holds more than one point, otherwise point", example = "cluster")
	private String            kind;
	@Schema(description = "Number of points in the cluster", example = "17")
	private long              pointCount;
	@Schema(description = "Average latitude", example = "60.17")
	private Double            latitude;
	@Schema(description = "Average longitude", example = "24.94")
	private Double            longitude;
	@Schema(description = "Bounding box of the points in the cluster")
	private GpsBoundsResponse bounds;
	@Schema(description = "Bounding box of the grid cell")
	private GpsBoundsResponse cellBounds;
	@Schema(description = "A sample file name from the cluster", example = "IMG_0001.jpg")
	private String            sampleFilename;
	@Schema(description = "Earliest timestamp in the cluster", example = "2024-05-13T16:03:44")
	private LocalDateTime     firstTimestamp;
	@Schema(description = "Latest timestamp in the cluster", example = "2024-05-13T18:03:44")
	private LocalDateTime     lastTimestamp;
}
