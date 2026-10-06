package fi.poltsi.vempain.website.api;

import fi.poltsi.vempain.website.api.request.LoginRequest;
import fi.poltsi.vempain.website.api.request.SubjectIdSearchRequest;
import fi.poltsi.vempain.website.api.request.SubjectSearchRequest;
import fi.poltsi.vempain.website.api.response.ApiErrorResponse;
import fi.poltsi.vempain.website.api.response.DirectoryNodeResponse;
import fi.poltsi.vempain.website.api.response.EmbedItemResponse;
import fi.poltsi.vempain.website.api.response.GpsBoundsResponse;
import fi.poltsi.vempain.website.api.response.GpsClusterItemResponse;
import fi.poltsi.vempain.website.api.response.GpsClusterPointsResponse;
import fi.poltsi.vempain.website.api.response.GpsClustersResponse;
import fi.poltsi.vempain.website.api.response.GpsOverviewResponse;
import fi.poltsi.vempain.website.api.response.GpsPointResponse;
import fi.poltsi.vempain.website.api.response.GpsTrackResponse;
import fi.poltsi.vempain.website.api.response.LastItemResponse;
import fi.poltsi.vempain.website.api.response.LastItemsResponse;
import fi.poltsi.vempain.website.api.response.LocationResponse;
import fi.poltsi.vempain.website.api.response.LoginResponse;
import fi.poltsi.vempain.website.api.response.MusicDataResponse;
import fi.poltsi.vempain.website.api.response.MusicDataRowResponse;
import fi.poltsi.vempain.website.api.response.PageDirectoryResponse;
import fi.poltsi.vempain.website.api.response.PageEmbedResponse;
import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.StatusResponse;
import fi.poltsi.vempain.website.api.response.SubjectResponse;
import fi.poltsi.vempain.website.api.response.SubjectSearchResponse;
import fi.poltsi.vempain.website.api.response.WebSiteFileResponse;
import fi.poltsi.vempain.website.api.response.WebSiteGalleryResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.api.response.WordCloudEntryResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the JSON contract of every DTO to the field names of the matching TypeScript model in
 * {@code vempain-website-frontend/src/models} (and {@code vempain-rt-renderer/src/types.ts}).
 */
class ResponseContractJTC {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void pageMatchesTheWebSitePageModel() {
		assertEquals(Set.of("id", "page_id", "title", "header", "body", "page_style", "secure", "acl_id", "file_path", "creator", "created",
							"modifier", "modified", "published", "embeds", "subjects"),
					 keys(WebSitePageResponse.builder()
											 .published(LocalDateTime.of(2024, 5, 15, 12, 0))
											 .build()));
	}

	@Test
	void pageEmbedMatchesThePageEmbedModel() {
		assertEquals(Set.of("type", "embed_id", "hero_type", "hero_duration", "hero_transition", "identifier", "word_cloud_options",
							"today_random_options", "placeholder", "autoplay", "dot_duration", "speed", "items", "youtube_url", "last_type", "count"),
					 keys(PageEmbedResponse.builder()
										   .build()));
		assertEquals(Set.of("title", "body"), keys(EmbedItemResponse.builder()
																	.build()));
	}

	@Test
	void fileMatchesTheWebSiteFileModel() {
		String json = json(WebSiteFileResponse.builder()
											  .originalDateTime(OffsetDateTime.parse("2024-05-13T16:03:44Z"))
											  .build());
		assertEquals(Set.of("id", "file_id", "acl_id", "comment", "file_path", "thumbnail_path", "mimetype", "original_date_time", "rights_holder",
							"rights_terms", "rights_url", "creator_name", "creator_email", "creator_country", "creator_url", "location", "location_id",
							"width", "height", "length", "pages", "metadata", "subjects"),
					 keys(json));
		assertTrue(json.contains("\"original_date_time\":\"2024-05-13T16:03:44Z\""));
	}

	@Test
	void locationMatchesTheWebSiteLocationModel() {
		assertEquals(Set.of("id", "latitude", "latitude_ref", "longitude", "longitude_ref", "altitude", "direction", "satellite_count", "country", "state",
							"city", "street", "sub_location"),
					 keys(LocationResponse.builder()
										  .build()));
	}

	@Test
	void galleryAndSubjectMatchTheirModels() {
		assertEquals(Set.of("id", "gallery_id", "shortname", "description", "acl_id", "subjects"), keys(WebSiteGalleryResponse.builder()
																															  .build()));
		assertEquals(Set.of("id", "subject", "subject_de", "subject_en", "subject_es", "subject_fi", "subject_se"), keys(SubjectResponse.builder()
																																		.build()));
		assertEquals(Set.of("pages", "galleries", "files"), keys(SubjectSearchResponse.builder()
																					  .build()));
	}

	@Test
	void pagedResponseMatchesTheSharedPagedResponseModel() {
		PagedResponse<String> response = PagedResponse.of(List.of("a"), 1, 2, 5);

		assertEquals(Set.of("content", "page", "size", "total_elements", "total_pages", "first", "last", "empty"), keys(response));
		assertEquals(3, response.getTotalPages());
		assertFalse(response.isFirst());
		assertFalse(response.isLast());
		assertTrue(PagedResponse.of(List.of(), 0, 10, 0)
								.isLast());
	}

	@Test
	void directoryModelsMatch() {
		assertEquals(Set.of("name"), keys(PageDirectoryResponse.builder()
															   .build()));
		assertEquals(Set.of("title", "key", "is_leaf", "children"), keys(DirectoryNodeResponse.builder()
																							  .build()));
	}

	@Test
	void musicModelsMatch() {
		assertEquals(Set.of("identifier", "items", "page", "size", "total_elements", "total_pages", "first", "last", "sort_by", "direction", "search"),
					 keys(MusicDataResponse.builder()
										   .build()));
		assertEquals(Set.of("id", "artist", "album_artist", "album", "year", "track_number", "track_total", "track_name", "genre", "duration_seconds"),
					 keys(MusicDataRowResponse.builder()
											  .build()));
	}

	@Test
	void gpsModelsMatch() {
		assertEquals(Set.of("min_latitude", "max_latitude", "min_longitude", "max_longitude"), keys(GpsBoundsResponse.builder()
																													 .build()));
		assertEquals(Set.of("identifier", "point_count", "bounds"), keys(GpsOverviewResponse.builder()
																							.build()));
		assertEquals(Set.of("cluster_key", "kind", "point_count", "latitude", "longitude", "bounds", "cell_bounds", "sample_filename", "first_timestamp",
							"last_timestamp"),
					 keys(GpsClusterItemResponse.builder()
												.build()));
		assertEquals(Set.of("identifier", "zoom", "items", "bounds"), keys(GpsClustersResponse.builder()
																							  .build()));
		assertEquals(Set.of("id", "timestamp", "latitude", "longitude", "altitude", "filename"), keys(GpsPointResponse.builder()
																													  .build()));
		assertEquals(Set.of("identifier", "cluster_key", "bounds", "items"), keys(GpsClusterPointsResponse.builder()
																										  .build()));
		assertEquals(Set.of("identifier", "total_points", "sampled_points", "sample_step", "items"), keys(GpsTrackResponse.builder()
																														  .build()));
	}

	@Test
	void lastItemsAndWordCloudModelsMatch() {
		assertEquals(Set.of("type", "count", "items"), keys(LastItemsResponse.builder()
																			 .build()));
		assertEquals(Set.of("id", "title", "published", "file_path", "thumbnail_path", "gallery_id", "header", "body"), keys(LastItemResponse.builder()
																																			 .build()));
		assertEquals(Set.of("text", "value"), keys(WordCloudEntryResponse.builder()
																		 .build()));
	}

	@Test
	void simpleResponsesMatch() {
		assertEquals(Set.of("token"), keys(LoginResponse.builder()
														.build()));
		assertEquals(Set.of("status"), keys(StatusResponse.builder()
														  .build()));
		assertEquals(Set.of("error", "code"), keys(ApiErrorResponse.builder()
																   .build()));
	}

	@Test
	void requestsAcceptTheSnakeCasePayloadsSentByTheFrontend() {
		SubjectIdSearchRequest byIds = objectMapper.readValue("""
																	  {"subject_ids":[4,5],"page":1,"size":20,"sort_by":"id","direction":"ASC"}
																	  """, SubjectIdSearchRequest.class);
		assertEquals(List.of(4L, 5L), byIds.getSubjectIds());
		assertEquals(1, byIds.getPage());
		assertEquals("id", byIds.getSortBy());

		SubjectSearchRequest search = objectMapper.readValue("""
																	 {"page":0,"size":12,"sort_by":"id","direction":"ASC","search":"sunset","case_sensitive":false}
																	 """, SubjectSearchRequest.class);
		assertEquals("sunset", search.getSearch());
		assertEquals(Boolean.FALSE, search.getCaseSensitive());

		LoginRequest login = objectMapper.readValue("""
															{"username":"alice","password":"secret","unknown":"ignored"}
															""", LoginRequest.class);
		assertEquals("alice", login.getUsername());
	}

	private Set<String> keys(Object dto) {
		return keys(json(dto));
	}

	private String json(Object dto) {
		return objectMapper.writeValueAsString(dto);
	}

	private Set<String> keys(String json) {
		JsonNode    node = objectMapper.readTree(json);
		Set<String> keys = new LinkedHashSet<>();
		node.propertyNames()
			.forEach(keys::add);
		return keys;
	}
}
