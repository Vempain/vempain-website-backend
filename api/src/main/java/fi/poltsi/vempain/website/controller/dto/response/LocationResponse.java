package fi.poltsi.vempain.website.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * GPS location and reverse-geocoded metadata.
 *
 * @param id             location identifier
 * @param latitude       latitude in decimal degrees
 * @param latitudeRef    latitude hemisphere reference
 * @param longitude      longitude in decimal degrees
 * @param longitudeRef   longitude hemisphere reference
 * @param altitude       altitude in meters
 * @param direction      direction in degrees
 * @param satelliteCount number of satellites used
 * @param country        country name
 * @param state          state or region
 * @param city           city name
 * @param street         street name
 * @param subLocation    more specific location
 */
@Schema(name = "LocationResponse", description = "GPS location and reverse-geocoded metadata")
public record LocationResponse(
		@Schema(description = "Location identifier", example = "42")
		Long id,
		@Schema(description = "Latitude in decimal degrees", example = "60.16952")
		BigDecimal latitude,
		@Schema(description = "Latitude hemisphere reference", example = "N")
		@JsonProperty("latitude_ref") String latitudeRef,
		@Schema(description = "Longitude in decimal degrees", example = "24.93545")
		BigDecimal longitude,
		@Schema(description = "Longitude hemisphere reference", example = "E")
		@JsonProperty("longitude_ref") String longitudeRef,
		@Schema(description = "Altitude in meters", example = "25.5")
		Double altitude,
		@Schema(description = "Direction in degrees", example = "180.0")
		Double direction,
		@Schema(description = "Number of satellites used", example = "8")
		@JsonProperty("satellite_count") Integer satelliteCount,
		@Schema(description = "Country name", example = "Finland")
		String country,
		@Schema(description = "State or region", example = "Uusimaa")
		String state,
		@Schema(description = "City name", example = "Helsinki")
		String city,
		@Schema(description = "Street name", example = "Mannerheimintie")
		String street,
		@Schema(description = "More specific location", example = "Central Park")
		@JsonProperty("sub_location") String subLocation
) {

}
