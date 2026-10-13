package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.entity.WebSiteSubject;
import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

	@Test
	void autocompleteMapsSubjectsAndWordCloudMapsTagCountsWithTheCountCapped() throws Exception {
		WebSiteSubject subject = mock(WebSiteSubject.class);
		when(subject.getId()).thenReturn(5L);
		when(subject.getSubject()).thenReturn("sunset");
		when(subject.getSubjectFi()).thenReturn("auringonlasku");
		when(subjects.autocomplete("%sun%", Limit.of(20))).thenReturn(List.of(subject));
		WebSiteSubjectRepository.TagCount tag = mock(WebSiteSubjectRepository.TagCount.class);
		when(tag.getText()).thenReturn("sunset");
		when(tag.getValue()).thenReturn(3L);
		when(subjects.findMostUsedTags(200)).thenReturn(List.of(tag));

		MockMvcBuilders.standaloneSetup(new SubjectController(subjects))
		               .build()
					   .perform(get("/api/public/subjects/autocomplete").param("q", "Sun"))
					   .andExpect(status().isOk())
					   .andExpect(content().json("""
														 [{"id":5,"subject":"sunset","subject_fi":"auringonlasku","subject_en":null}]
														 """));
		MockMvcBuilders.standaloneSetup(new SubjectController(subjects))
		               .build()
					   .perform(get("/api/public/embeds/word-cloud").param("count", "5000"))
					   .andExpect(status().isOk())
					   .andExpect(content().json("""
														 [{"text":"sunset","value":3}]
														 """));
	}
}
