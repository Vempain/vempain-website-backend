package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import fi.poltsi.vempain.website.service.PublishedDataService;
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
class EmbedControllerCTC {
	@Mock PublishedDataService data;
	@Mock WebSitePageRepository pages;
	@Mock WebSiteFileRepository files;
	@Mock WebSiteGalleryRepository galleries;
	@Mock CurrentUserProvider user;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new EmbedController(data, pages, files, galleries, user))
		                     .setControllerAdvice(new ApiExceptionHandler()).build();
	}

	@Test
	void publishedDataEndpointsReturnTheirJsonContracts() throws Exception {
		when(data.music(anyString(), anyInt(), anyInt(), anyString(), anyString(), anyString()))
				.thenReturn(Map.of("identifier", "music", "items", List.of()));
		when(data.gpsOverview("music")).thenReturn(Map.of("identifier", "music", "point_count", 0));
		when(data.gpsPoints(anyString(), anyInt())).thenReturn(List.of());
		when(data.gpsClusters(anyString(), anyInt(), any(), any(), any(), any()))
				.thenReturn(Map.of("identifier", "music", "items", List.of()));
		when(user.currentUserId()).thenReturn(-1L);

		mvc.perform(get("/api/public/embeds/music/music"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"identifier":"music","items":[]}
						"""));
		mvc.perform(get("/api/public/embeds/gps/music/overview"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"identifier":"music","point_count":0}
						"""));
		mvc.perform(get("/api/public/embeds/gps/music/track"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
		mvc.perform(get("/api/public/embeds/gps/music/clusters"))
				.andExpect(status().isOk());
		mvc.perform(get("/api/public/embeds/gps/music/clusters/1:2:3/points"))
				.andExpect(status().isOk());
		mvc.perform(get("/api/public/embeds/last").param("type", "unknown"))
				.andExpect(status().isBadRequest())
				.andExpect(content().json("""
						{"error":"Unsupported last-items type"}
						"""));
	}

	@Test
	void clusterPointsRejectMalformedKeys() throws Exception {
		mvc.perform(get("/api/public/embeds/gps/music/clusters/bad/points"))
				.andExpect(status().isBadRequest())
				.andExpect(content().json("""
						{"error":"Invalid cluster key"}
						"""));
	}
}
