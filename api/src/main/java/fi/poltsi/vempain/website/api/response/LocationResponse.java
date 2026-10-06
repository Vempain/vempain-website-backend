package fi.poltsi.vempain.website.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "LocationResponse", description = "GPS location and reverse-geocoded metadata of a file; mirrors the frontend WebSiteLocation model")
public class LocationResponse {
	@Schema(description = "Location identifier", example = "42")
	private Long       id;
	@Schema(description = "Latitude in decimal degrees", example = "60.16952")
	private BigDecimal latitude;
	@Schema(description = "Latitude hemisphere reference", example = "N")
	private String     latitudeRef;
	@Schema(description = "Longitude in decimal degrees", example = "24.93545")
	private BigDecimal longitude;
	@Schema(description = "Longitude hemisphere reference", example = "E")
	private String     longitudeRef;
	@Schema(description = "Altitude in meters", example = "25.5")
	private Double     altitude;
	@Schema(description = "Direction in degrees", example = "180.0")
	private Double     direction;
	@Schema(description = "Number of satellites used", example = "8")
	private Integer    satelliteCount;
	@Schema(description = "Country name", example = "Finland")
	private String     country;
	@Schema(description = "State or region", example = "Uusimaa")
	private String     state;
	@Schema(description = "City name", example = "Helsinki")
	private String     city;
	@Schema(description = "Street name", example = "Mannerheimintie")
	private String     street;
	@Schema(description = "More specific location", example = "Central Park")
	private String     subLocation;
}
