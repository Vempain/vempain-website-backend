package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SubjectControllerCTC {
	@Mock WebSiteSubjectRepository subjects;

	@Test
	void subjectEndpointsReturnArraysAndClampInvalidCount() throws Exception {
		when(subjects.autocomplete(anyString(), any())).thenReturn(List.of());
		when(subjects.findMostUsedTags(1)).thenReturn(List.of());
		MockMvcBuilders.standaloneSetup(new SubjectController(subjects)).build()
				.perform(get("/api/public/subjects/autocomplete").param("q", "  Alpha "))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
		MockMvcBuilders.standaloneSetup(new SubjectController(subjects)).build()
				.perform(get("/api/public/embeds/word-cloud").param("count", "-4"))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}
}
