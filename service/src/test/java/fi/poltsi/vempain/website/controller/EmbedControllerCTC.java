package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.GpsBoundsResponse;
import fi.poltsi.vempain.website.api.response.GpsClusterPointsResponse;
import fi.poltsi.vempain.website.api.response.GpsClustersResponse;
import fi.poltsi.vempain.website.api.response.GpsOverviewResponse;
import fi.poltsi.vempain.website.api.response.GpsTrackResponse;
import fi.poltsi.vempain.website.api.response.LastItemResponse;
import fi.poltsi.vempain.website.api.response.LastItemsResponse;
import fi.poltsi.vempain.website.api.response.MusicDataResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.service.LastItemsService;
import fi.poltsi.vempain.website.service.PublishedDataService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmbedControllerCTC {
	@Mock PublishedDataService data;
	@Mock
	LastItemsService    lastItems;
	@Mock
	CurrentUserProvider user;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new EmbedController(data, lastItems, user))
							 .setControllerAdvice(new ApiExceptionHandler())
							 .build();
	}

	@Test
	void publishedDataEndpointsReturnTheirSnakeCaseContracts() throws Exception {
		when(data.music(anyString(), anyInt(), anyInt(), anyString(), anyString(), anyString()))
				.thenReturn(MusicDataResponse.builder()
											 .identifier("music")
											 .items(List.of())
											 .totalElements(0)
											 .sortBy("artist")
											 .first(true)
											 .last(true)
											 .build());
		when(data.gpsOverview("music")).thenReturn(GpsOverviewResponse.builder()
																	  .identifier("music")
																	  .pointCount(0)
																	  .build());
		when(data.gpsTrack("music", 3000)).thenReturn(GpsTrackResponse.builder()
																	  .identifier("music")
																	  .items(List.of())
																	  .build());
		when(data.gpsClusters(eq("music"), eq(4), any(), any(), any(), any())).thenReturn(GpsClustersResponse.builder()
																											 .identifier("music")
																											 .zoom(4)
																											 .items(List.of())
																											 .build());
		when(data.gpsClusterPoints("music", "1:2:3", 250)).thenReturn(GpsClusterPointsResponse.builder()
																							  .identifier("music")
																							  .clusterKey("1:2:3")
																							  .bounds(GpsBoundsResponse.builder()
																													   .minLatitude(-60.0)
																													   .build())
																							  .items(List.of())
																							  .build());

		mvc.perform(get("/api/public/embeds/music/music"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 {"identifier":"music","items":[],"total_elements":0,"sort_by":"artist","first":true,"last":true}
											 """));
		mvc.perform(get("/api/public/embeds/gps/music/overview"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 {"identifier":"music","point_count":0,"bounds":null}
											 """));
		mvc.perform(get("/api/public/embeds/gps/music/track"))
		   .andExpect(status().isOk())
		   .andExpect(jsonPath("$.items").isArray())
		   .andExpect(jsonPath("$.sample_step").exists());
		mvc.perform(get("/api/public/embeds/gps/music/clusters"))
		   .andExpect(status().isOk())
		   .andExpect(jsonPath("$.zoom").value(4));
		mvc.perform(get("/api/public/embeds/gps/music/clusters/1:2:3/points"))
		   .andExpect(status().isOk())
		   .andExpect(jsonPath("$.cluster_key").value("1:2:3"))
		   .andExpect(jsonPath("$.bounds.min_latitude").value(-60.0));
	}

	@Test
	void lastItemsDelegateToTheServiceWithTheCurrentUser() throws Exception {
		when(user.currentUserId()).thenReturn(7L);
		when(lastItems.latest("images", 5, 7L)).thenReturn(LastItemsResponse.builder()
																			.type("images")
																			.count(5)
																			.items(List.of(LastItemResponse.builder()
																										   .id(1L)
																										   .title("a.jpg")
																										   .filePath("images/a.jpg")
																										   .thumbnailPath("images/.thumb/a.jpg")
																										   .build()))
																			.build());

		mvc.perform(get("/api/public/embeds/last").param("type", "images"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 {"type":"images","count":5,"items":[{"id":1,"title":"a.jpg","published":null,"file_path":"images/a.jpg",
											 "thumbnail_path":"images/.thumb/a.jpg","gallery_id":null,"header":null,"body":null}]}
											 """));
	}

	@Test
	void unsupportedLastItemTypeIsABadRequest() throws Exception {
		when(user.currentUserId()).thenReturn(-1L);
		when(lastItems.latest("unknown", 5, -1L)).thenThrow(ApiException.badRequest("Unsupported last-items type"));

		mvc.perform(get("/api/public/embeds/last").param("type", "unknown"))
		   .andExpect(status().isBadRequest())
		   .andExpect(content().json("""
											 {"error":"Unsupported last-items type"}
											 """));
	}

	@Test
	void clusterPointsRejectMalformedKeysBeforeTouchingTheData() throws Exception {
		mvc.perform(get("/api/public/embeds/gps/music/clusters/bad/points"))
		   .andExpect(status().isBadRequest())
		   .andExpect(content().json("""
											 {"error":"Invalid cluster key"}
											 """));
		verify(data, never()).gpsClusterPoints(anyString(), anyString(), anyInt());
	}
}
