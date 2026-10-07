package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.SubjectSearchResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.service.PageService;
import fi.poltsi.vempain.website.service.SubjectSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubjectSearchControllerCTC {
	@Mock
	        PageService          pages;
	@Mock
	        SubjectSearchService subjectSearch;
	private MockMvc              mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new SubjectSearchController(pages, subjectSearch))
							 .setControllerAdvice(new ApiExceptionHandler())
							 .build();
	}

	@Test
	void searchEndpointsAcceptTheFrontendPayloads() throws Exception {
		when(pages.list(anyInt(), anyInt(), anyString(), anyString(), any(), anyLong()))
				.thenReturn(PagedResponse.of(List.of(WebSitePageResponse.builder()
																		.id(1L)
																		.title("Sunset")
																		.build()), 0, 25, 1));
		when(subjectSearch.search(anyList(), anyInt(), anyInt(), anyLong()))
				.thenReturn(SubjectSearchResponse.builder()
												 .pages(PagedResponse.of(List.of(), 1, 20, 0))
												 .galleries(PagedResponse.of(List.of(), 1, 20, 0))
												 .files(PagedResponse.of(List.of(), 1, 20, 0))
												 .build());

		mvc.perform(post("/api/public/subject-search").contentType(MediaType.APPLICATION_JSON)
													  .content("""
																	   {"page":0,"size":25,"sort_by":"id","direction":"ASC","search":"sunset","case_sensitive":false}
																	   """))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 {"content":[{"id":1,"title":"Sunset"}],"page":0,"size":25,"total_elements":1,"total_pages":1,
											 "first":true,"last":true,"empty":false}
											 """));
		mvc.perform(post("/api/public/subjects/search").contentType(MediaType.APPLICATION_JSON)
													   .content("""
																		{"page":1,"size":20,"subject_ids":[4,5],"sort_by":"id","direction":"ASC"}
																		"""))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 {"pages":{"content":[],"page":1,"size":20},"galleries":{"content":[]},"files":{"content":[]}}
											 """));
		verify(pages).list(0, 25, "asc", "sunset", null, -1L);
		verify(subjectSearch).search(List.of(4L, 5L), 1, 20, -1L);
	}

	@Test
	void missingBodiesFallBackToDefaults() throws Exception {
		when(pages.list(anyInt(), anyInt(), anyString(), anyString(), any(), anyLong())).thenReturn(PagedResponse.of(List.of(), 0, 12, 0));
		when(subjectSearch.search(anyList(), anyInt(), anyInt(), anyLong())).thenReturn(SubjectSearchResponse.builder()
																											 .build());

		mvc.perform(post("/api/public/subject-search").contentType(MediaType.APPLICATION_JSON)
													  .content("{}"))
		   .andExpect(status().isOk());
		mvc.perform(post("/api/public/subjects/search").contentType(MediaType.APPLICATION_JSON)
													   .content("{}"))
		   .andExpect(status().isOk());
		verify(pages).list(0, 12, "asc", "", null, -1L);
		verify(subjectSearch).search(List.of(), 0, 12, -1L);
	}

	@Test
	void constraintViolationsAreRejectedWithTheErrorContract() throws Exception {
		// Jakarta Validation on the request DTOs: overlong search text, out-of-range paging and non-positive ids never reach the services
		mvc.perform(post("/api/public/subject-search").contentType(MediaType.APPLICATION_JSON)
													  .content("{\"search\":\"" + "a".repeat(201) + "\"}"))
		   .andExpect(status().isBadRequest())
		   .andExpect(content().json("""
											 {"error":"Invalid request"}
											 """));
		mvc.perform(post("/api/public/subject-search").contentType(MediaType.APPLICATION_JSON)
													  .content("{\"page\":-1,\"size\":500}"))
		   .andExpect(status().isBadRequest());
		mvc.perform(post("/api/public/subjects/search").contentType(MediaType.APPLICATION_JSON)
													   .content("""
																		{"subject_ids":[4,0,-1]}
																		"""))
		   .andExpect(status().isBadRequest())
		   .andExpect(content().json("""
											 {"error":"Invalid request"}
											 """));
		verifyNoInteractions(pages, subjectSearch);
	}

	@Test
	void malformedBodiesAreRejectedWithTheErrorContract() throws Exception {
		mvc.perform(post("/api/public/subject-search").contentType(MediaType.APPLICATION_JSON)
													  .content("""
																	   {"page":"wrong","size":25,"search":"sunset"}
																	   """))
		   .andExpect(status().isBadRequest())
		   .andExpect(content().json("""
											 {"error":"Malformed request"}
											 """));
		mvc.perform(post("/api/public/subjects/search").contentType(MediaType.APPLICATION_JSON)
													   .content("""
																		{"subject_ids":"not-a-list"}
																		"""))
		   .andExpect(status().isBadRequest());
		verifyNoInteractions(pages, subjectSearch);
	}
}
