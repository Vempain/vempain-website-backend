package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.service.PageService;
import fi.poltsi.vempain.website.controller.dto.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PageControllerCTC {
	@Mock PageService pages;
	@Mock CurrentUserProvider user;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new PageController(pages, user))
		                     .setControllerAdvice(new ApiExceptionHandler()).build();
	}

	@Test
	void pageRoutesReturnPagedAndTreeJson() throws Exception {
		when(user.currentUserId()).thenReturn(-1L);
		when(pages.list(anyInt(), anyInt(), anyString(), anyString(), any(), anyLong()))
				.thenReturn(PagedResponse.of(List.of(), 0, 12, 0));
		when(pages.children(4L)).thenReturn(List.of());
		when(pages.directories()).thenReturn(List.of());
		when(pages.directoryTree("photos", -1L)).thenReturn(List.of());
		WebSitePage page = mock(WebSitePage.class);
		when(pages.byId(4L)).thenReturn(page);
		when(pages.page(page)).thenReturn(Map.of("title", "Page"));

		mvc.perform(get("/api/pages"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"content":[],"page":0,"size":12,"total_elements":0,"total_pages":0,"first":true,"last":true,"empty":true}
						"""));
		mvc.perform(get("/api/pages/4"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"title":"Page"}
						"""));
		mvc.perform(get("/api/public/pages"))
				.andExpect(status().isOk());
		mvc.perform(get("/api/public/pages/4/children"))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
		mvc.perform(get("/api/public/page-directories"))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
		mvc.perform(get("/api/public/page-directories/photos/tree"))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void pageRoutesRejectMissingOrWrongInput() throws Exception {
		mvc.perform(get("/api/public/page-content"))
				.andExpect(status().is4xxClientError());
		mvc.perform(get("/api/pages/not-a-number"))
				.andExpect(status().is4xxClientError());
	}
}
