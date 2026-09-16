package fi.poltsi.vempain.website.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNull;

class CorsFilterConfigurationUTC {

	@Test
	void emptyOriginAllowListDoesNotReflectCredentialsOrigin() throws Exception {
		SiteProperties properties = new SiteProperties();
		properties.getCorsAllowedOrigins()
		          .clear();
		FilterRegistrationBean<?> registration =
				new CorsFilterConfiguration().corsFilterRegistration(properties);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Origin", "https://attacker.example");
		MockHttpServletResponse response = new MockHttpServletResponse();

		registration.getFilter()
		            .doFilter(request, response, new MockFilterChain());

		assertNull(response.getHeader("Access-Control-Allow-Origin"));
	}
}
