package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.DirectoryNodeResponse;
import fi.poltsi.vempain.website.api.response.PageDirectoryResponse;
import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.service.PageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PageControllerCTC {
	@Mock
	PageService pages;
	@Mock CurrentUserProvider user;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new PageController(pages, user))
							 .setControllerAdvice(new ApiExceptionHandler())
							 .build();
	}

	@Test
	void pageRoutesReturnTypedPagedAndTreeJson() throws Exception {
		when(user.currentUserId()).thenReturn(-1L);
		when(pages.list(anyInt(), anyInt(), anyString(), anyString(), any(), anyLong()))
				.thenReturn(PagedResponse.of(List.of(), 0, 12, 0));
		when(pages.children(4L, -1L)).thenReturn(List.of());
		when(pages.directories()).thenReturn(List.of(PageDirectoryResponse.builder()
																		  .name("photos")
																		  .build()));
		when(pages.directoryTree("photos", -1L)).thenReturn(List.of(DirectoryNodeResponse.builder()
																						 .title("summer")
																						 .key("photos/summer")
																						 .isLeaf(true)
																						 .build()));
		WebSitePage page = mock(WebSitePage.class);
		when(pages.byId(4L)).thenReturn(page);
		when(pages.page(page)).thenReturn(WebSitePageResponse.builder()
															 .id(4L)
															 .pageId(40L)
															 .title("Page")
															 .body("<p>Body</p>")
															 .build());

		mvc.perform(get("/api/pages"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 {"content":[],"page":0,"size":12,"total_elements":0,"total_pages":0,"first":true,"last":true,"empty":true}
											 """));
		mvc.perform(get("/api/pages/4"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 {"id":4,"page_id":40,"title":"Page","body":"<p>Body</p>","secure":false,"acl_id":null}
											 """));
		mvc.perform(get("/api/public/pages"))
		   .andExpect(status().isOk());
		mvc.perform(get("/api/public/pages/4/children"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("[]"));
		mvc.perform(get("/api/public/page-directories"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 [{"name":"photos"}]
											 """));
		mvc.perform(get("/api/public/page-directories/photos/tree"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 [{"title":"summer","key":"photos/summer","is_leaf":true,"children":null}]
											 """));
	}

	@Test
	void deniedAndMissingPagesUseTheErrorContract() throws Exception {
		when(pages.byId(9L)).thenReturn(null);
		doThrow(ApiException.notFound("Page not found")).when(pages)
														.require(null);

		mvc.perform(get("/api/pages/9"))
		   .andExpect(status().isNotFound())
		   .andExpect(content().json("""
											 {"error":"Page not found","code":null}
											 """));
	}

	@Test
	void pageRoutesRejectMissingOrWrongInput() throws Exception {
		mvc.perform(get("/api/public/page-content"))
		   .andExpect(status().is4xxClientError());
		mvc.perform(get("/api/pages/not-a-number"))
		   .andExpect(status().is4xxClientError());
	}
}
