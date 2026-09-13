package fi.poltsi.vempain.website.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HealthControllerCTC {
	@Test
	void healthReturnsOkJson() throws Exception {
		MockMvcBuilders.standaloneSetup(new HealthController()).build()
				.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"status":"ok"}
						"""));
	}
}
