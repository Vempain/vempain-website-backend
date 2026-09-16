package fi.poltsi.vempain.website.config;

import fi.poltsi.vempain.website.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityHeadersFilterCTC {

	@Test
	void addsBrowserHardeningHeadersToResponses() throws Exception {
		MockMvcBuilders.standaloneSetup(new HealthController())
		               .addFilters(new SecurityHeadersFilter())
		               .build()
		               .perform(get("/health").secure(true))
		               .andExpect(status().isOk())
		               .andExpect(header().string("X-Content-Type-Options", "nosniff"))
		               .andExpect(header().string("X-Frame-Options", "DENY"))
		               .andExpect(header().string("Strict-Transport-Security",
		                                          "max-age=31536000; includeSubDomains"))
		               .andExpect(header().string("Content-Security-Policy",
		                                          "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'"));
	}
}
