package fi.poltsi.vempain.website.entity;

import java.time.LocalDateTime;

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

    /**
     * Nick of the admin user who created the row on the site side; null for rows published before the audit columns existed.
     */
    @Column(name = "creator", length = 512)
    private String creator;

    @Column(name = "created")
    private LocalDateTime created;

    /**
     * Nick of the admin user who last changed the row on the site side.
     */
    @Column(name = "modifier", length = 512)
    private String modifier;

    @Column(name = "modified")
    private LocalDateTime modified;

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

    public String getCreator() {
        return creator;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public String getModifier() {
        return modifier;
    }

    public LocalDateTime getModified() {
        return modified;
    }
}
