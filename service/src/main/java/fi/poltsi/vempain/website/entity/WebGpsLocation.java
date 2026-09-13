package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * GPS location attached to a file. The identifier is assigned by the publisher, so no
 * generation strategy is declared.
 */
@Entity
@Table(name = "web_gps_location")
public class WebGpsLocation {

	@Id
	@Column(name = "id")
	private Long id;

	@Column(name = "latitude", nullable = false, precision = 15, scale = 5)
	private BigDecimal latitude;

	@Column(name = "latitude_ref", nullable = false, length = 1)
	private String latitudeRef;

	@Column(name = "longitude", nullable = false, precision = 15, scale = 5)
	private BigDecimal longitude;

	@Column(name = "longitude_ref", nullable = false, length = 1)
	private String longitudeRef;

	@Column(name = "altitude")
	private Double altitude;

	@Column(name = "direction")
	private Double direction;

	@Column(name = "satellite_count")
	private Integer satelliteCount;

	@Column(name = "country", length = 255)
	private String country;

	@Column(name = "state", length = 255)
	private String state;

	@Column(name = "city", length = 255)
	private String city;

	@Column(name = "street", length = 255)
	private String street;

	@Column(name = "sub_location", length = 255)
	private String subLocation;

	public Long getId() {
		return id;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public String getLatitudeRef() {
		return latitudeRef;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public String getLongitudeRef() {
		return longitudeRef;
	}

	public Double getAltitude() {
		return altitude;
	}

	public Double getDirection() {
		return direction;
	}

	public Integer getSatelliteCount() {
		return satelliteCount;
	}

	public String getCountry() {
		return country;
	}

	public String getState() {
		return state;
	}

	public String getCity() {
		return city;
	}

	public String getStreet() {
		return street;
	}

	public String getSubLocation() {
		return subLocation;
	}
}
