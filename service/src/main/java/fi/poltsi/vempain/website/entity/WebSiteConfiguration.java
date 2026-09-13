package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "web_site_configuration")
public class WebSiteConfiguration {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "config_key", nullable = false, length = 255)
	private String configKey;

	@Column(name = "config_type", nullable = false)
	private String configType;

	@Column(name = "config_default", nullable = false)
	private String configDefault;

	@Column(name = "config_value", nullable = false)
	private String configValue;

	public Long getId() {
		return id;
	}

	public String getConfigKey() {
		return configKey;
	}

	public String getConfigType() {
		return configType;
	}

	public String getConfigDefault() {
		return configDefault;
	}

	public String getConfigValue() {
		return configValue;
	}
}
