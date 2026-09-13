package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.service.PageService;
import fi.poltsi.vempain.website.controller.dto.response.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SubjectSearchControllerCTC {
	@Mock PageService pages;

	@Test
	void searchEndpointsAcceptBlockJsonAndDefaultMalformedValues() throws Exception {
		when(pages.list(anyInt(), anyInt(), anyString(), anyString(), any(), anyLong()))
				.thenReturn(PagedResponse.of(List.of(), 0, 12, 0));
		var mvc = MockMvcBuilders.standaloneSetup(new SubjectSearchController(pages)).build();
		mvc.perform(post("/api/public/subject-search").contentType(MediaType.APPLICATION_JSON).content("""
				{"page":"wrong","size":25,"search":"sunset"}
				"""))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"content":[],"page":0,"size":12,"total_elements":0,"total_pages":0,"first":true,"last":true,"empty":true}
						"""));
		mvc.perform(post("/api/public/subjects/search").contentType(MediaType.APPLICATION_JSON).content("""
				{"page":1,"size":20}
				""")).andExpect(status().isOk());
		verify(pages).list(0, 25, "asc", "sunset", null, -1L);
	}
}
