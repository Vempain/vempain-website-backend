package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.service.PageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebsiteControllerCTC {
	@Mock PageService pages;

	@Test
	void websitePagesReturnJsonAndReservedPathsReturnNotFound() throws Exception {
		WebSitePage page = mock(WebSitePage.class);
		when(pages.byPath("home")).thenReturn(page);
		when(pages.page(page)).thenReturn(WebSitePageResponse.builder()
															 .title("Home")
															 .filePath("home")
															 .build());
		var mvc = MockMvcBuilders.standaloneSetup(new WebsiteController(pages))
								 .setControllerAdvice(new ApiExceptionHandler())
								 .build();
		mvc.perform(get("/home"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 {"title":"Home","file_path":"home"}
											 """));
		mvc.perform(get("/health"))
		   .andExpect(status().isNotFound())
		   .andExpect(content().json("""
											 {"error":"Page not found"}
											 """));
	}
}
