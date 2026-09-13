package fi.poltsi.vempain.website.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

	private static final String COOKIE_AUTH_SCHEME = "cookieAuth";

	@Bean
	public OpenAPI websiteOpenAPI(SiteProperties siteProperties) {
		return new OpenAPI()
				.components(new Components()
						.addSecuritySchemes(COOKIE_AUTH_SCHEME, new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.COOKIE)
								.name(siteProperties.getJwtCookieName())))
				.addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH_SCHEME));
	}
}
