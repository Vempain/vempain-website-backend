package fi.poltsi.vempain.website.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every failure reaches the caller as an {@code ApiErrorResponse}; unexpected failures and malformed input never echo details.
 */
class ApiExceptionHandlerCTC {
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new FailingController())
							 .setControllerAdvice(new ApiExceptionHandler())
							 .build();
	}

	@Test
	void apiExceptionsKeepTheirStatusAndMessage() throws Exception {
		mvc.perform(get("/fail/forbidden"))
		   .andExpect(status().isForbidden())
		   .andExpect(content().json("""
											 {"error":"Forbidden","code":null}
											 """));
	}

	@Test
	void illegalArgumentIsABadRequestAndIllegalStateIsNotFound() throws Exception {
		mvc.perform(get("/fail/illegal-argument"))
		   .andExpect(status().isBadRequest())
		   .andExpect(content().json("""
											 {"error":"Unsupported sort column"}
											 """));
		mvc.perform(get("/fail/illegal-state"))
		   .andExpect(status().isNotFound())
		   .andExpect(content().json("""
											 {"error":"Data set is not published"}
											 """));
	}

	@Test
	void malformedParametersAreReportedGenerically() throws Exception {
		mvc.perform(get("/fail/typed").param("count", "many"))
		   .andExpect(status().isBadRequest())
		   .andExpect(content().json("""
											 {"error":"Malformed request"}
											 """));
		mvc.perform(get("/fail/typed"))
		   .andExpect(status().isBadRequest())
		   .andExpect(content().json("""
											 {"error":"Malformed request"}
											 """));
	}

	@Test
	void unexpectedFailuresNeverLeakTheirMessage() throws Exception {
		mvc.perform(get("/fail/unexpected"))
		   .andExpect(status().isInternalServerError())
		   .andExpect(content().json("""
											 {"error":"Internal server error"}
											 """));
	}

	@RestController
	static class FailingController {

		@GetMapping("/fail/{kind}")
		String fail(@PathVariable String kind) {
			switch (kind) {
				case "forbidden" -> throw ApiException.forbidden("Forbidden");
				case "illegal-argument" -> throw new IllegalArgumentException("Unsupported sort column");
				case "illegal-state" -> throw new IllegalStateException("Data set is not published");
				default -> throw new RuntimeException("database password is hunter2");
			}
		}

		@GetMapping("/fail/typed")
		String typed(@RequestParam int count) {
			return String.valueOf(count);
		}
	}
}
