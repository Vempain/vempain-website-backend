package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.controller.dto.response.PagedResponse;
import fi.poltsi.vempain.website.controller.dto.response.SubjectSearchResponse;
import fi.poltsi.vempain.website.service.PageService;
import fi.poltsi.vempain.website.service.SubjectSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubjectSearchControllerCTC {
	@Mock PageService pages;
	@Mock
	SubjectSearchService subjectSearch;

	@Test
	void searchEndpointsAcceptBlockJsonAndDefaultMalformedValues() throws Exception {
		when(pages.list(anyInt(), anyInt(), anyString(), anyString(), any(), anyLong()))
				.thenReturn(PagedResponse.of(List.of(), 0, 12, 0));
		when(subjectSearch.search(anyList(), anyInt(), anyInt(), anyLong()))
				.thenReturn(new SubjectSearchResponse(PagedResponse.of(List.of(), 0, 20, 0),
													  PagedResponse.of(List.of(), 0, 20, 0), PagedResponse.of(List.of(), 0, 20, 0)));
		var mvc = MockMvcBuilders.standaloneSetup(new SubjectSearchController(pages, subjectSearch))
		                         .build();
		mvc.perform(post("/api/public/subject-search").contentType(MediaType.APPLICATION_JSON).content("""
				{"page":"wrong","size":25,"search":"sunset"}
				"""))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"content":[],"page":0,"size":12,"total_elements":0,"total_pages":0,"first":true,"last":true,"empty":true}
						"""));
		mvc.perform(post("/api/public/subjects/search").contentType(MediaType.APPLICATION_JSON).content("""
																												{"page":1,"size":20,"subject_ids":[4,5]}
				""")).andExpect(status().isOk());
		verify(pages).list(0, 25, "asc", "sunset", null, -1L);
		verify(subjectSearch).search(List.of(4L, 5L), 1, 20, -1L);
	}
}
