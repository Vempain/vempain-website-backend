package fi.poltsi.vempain.website.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SitePropertiesUTC {

    @Test
    void defaultsMatchTheDocumentedDeploymentValues() {
        SiteProperties properties = new SiteProperties();

        assertEquals("/files", properties.getFilesRoot());
        assertEquals("", properties.getJwtSecret());
        assertEquals(1200L, properties.getJwtTtlSeconds());
        assertEquals("OXALATE_JWT_TOKEN", properties.getJwtCookieName());
        assertTrue(properties.isJwtCookieSecure());
        assertTrue(properties.getCorsAllowedOrigins()
            .isEmpty());
        assertEquals("none", properties.getProxy()
            .getMode(), "proxy headers are not trusted unless configured");
        assertEquals("", properties.getProxy()
            .getTrustedHeader());
        assertEquals("", properties.getGeoip()
            .getDatabase());
        assertTrue(properties.getAccessLog()
            .isEnabled());
    }

    @Test
    void trafficSettingsAreBoundThroughTheNestedGroups() {
        SiteProperties properties = new SiteProperties();
        properties.getProxy()
            .setMode("cloudflare");
        properties.getProxy()
            .setTrustedHeader("True-Client-IP");
        properties.getGeoip()
            .setDatabase("/geoip/GeoLite2-Country.mmdb");
        properties.getAccessLog()
            .setEnabled(false);

        assertEquals("cloudflare", properties.getProxy()
            .getMode());
        assertEquals("True-Client-IP", properties.getProxy()
            .getTrustedHeader());
        assertEquals("/geoip/GeoLite2-Country.mmdb", properties.getGeoip()
            .getDatabase());
        assertFalse(properties.getAccessLog()
            .isEnabled());
    }

    @Test
    void boundValuesAreExposedThroughTheAccessors() {
        SiteProperties properties = new SiteProperties();
        properties.setFilesRoot("/srv/files");
        properties.setJwtSecret("secret");
        properties.setJwtTtlSeconds(60L);
        properties.setJwtCookieName("SITE_JWT");
        properties.setJwtCookieSecure(false);
        properties.setCorsAllowedOrigins(List.of("https://site.example"));

        assertEquals("/srv/files", properties.getFilesRoot());
        assertEquals("secret", properties.getJwtSecret());
        assertEquals(60L, properties.getJwtTtlSeconds());
        assertEquals("SITE_JWT", properties.getJwtCookieName());
        assertFalse(properties.isJwtCookieSecure());
        assertEquals(List.of("https://site.example"), properties.getCorsAllowedOrigins());
    }
}
