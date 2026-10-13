package fi.poltsi.vempain.website.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigurationUTC {

	@Test
	void documentsTheSessionCookieAsTheOnlySecurityScheme() {
		SiteProperties properties = new SiteProperties();
		properties.setJwtCookieName("SITE_JWT");

		OpenAPI openAPI = new OpenApiConfiguration().websiteOpenAPI(properties);

		SecurityScheme scheme = openAPI.getComponents()
									   .getSecuritySchemes()
									   .get("cookieAuth");
		assertEquals(1, openAPI.getComponents()
							   .getSecuritySchemes()
							   .size());
		assertEquals(SecurityScheme.Type.APIKEY, scheme.getType());
		assertEquals(SecurityScheme.In.COOKIE, scheme.getIn());
		assertEquals("SITE_JWT", scheme.getName());
		assertEquals(1, openAPI.getSecurity()
							   .size());
		assertTrue(openAPI.getSecurity()
						  .get(0)
						  .containsKey("cookieAuth"));
	}
}
