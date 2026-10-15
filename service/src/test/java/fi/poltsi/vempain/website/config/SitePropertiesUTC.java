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
