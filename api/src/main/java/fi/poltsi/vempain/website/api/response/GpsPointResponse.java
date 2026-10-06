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
@Schema(name = "GpsPointResponse", description = "Single GPS point; mirrors the frontend GpsPoint model")
public class GpsPointResponse {
	@Schema(description = "Point identifier", example = "1")
	private Long          id;
	@Schema(description = "Timestamp of the point", example = "2024-05-13T16:03:44")
	private LocalDateTime timestamp;
	@Schema(description = "Signed latitude", example = "60.17")
	private Double        latitude;
	@Schema(description = "Signed longitude", example = "24.94")
	private Double        longitude;
	@Schema(description = "Altitude in meters", example = "25.5")
	private Double        altitude;
	@Schema(description = "File name recorded with the point", example = "IMG_0001.jpg")
	private String        filename;
}
