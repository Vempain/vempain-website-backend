package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.api.response.SubjectResponse;
import fi.poltsi.vempain.website.api.response.WebSiteFileResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.auth.AuthenticatedUser;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebGpsLocation;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.repository.WebGpsLocationRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResponseMapperServiceUTC {
	private final SubjectLookupService     subjects    = mock(SubjectLookupService.class);
	private final WebGpsLocationRepository locations   = mock(WebGpsLocationRepository.class);
	private final CurrentUserProvider      currentUser = mock(CurrentUserProvider.class);
	private final ResponseMapperService    mapper      = new ResponseMapperService(subjects, locations, currentUser, new ObjectMapper());

	@Test
	void pagePrefersRenderedCacheParsesEmbedsAndIncludesAuditFields() {
		WebSitePage page = mock(WebSitePage.class);
		when(page.getId()).thenReturn(3L);
		when(page.getPageId()).thenReturn(30L);
		when(page.getTitle()).thenReturn("Title");
		when(page.getHeader()).thenReturn("Header");
		when(page.getCache()).thenReturn("<p>safe</p>");
		when(page.getBody()).thenReturn("source");
		when(page.getPageStyle()).thenReturn("body { color: red; }");
		when(page.getAclId()).thenReturn(2L);
		when(page.isSecure()).thenReturn(true);
		when(page.getFilePath()).thenReturn("trips/summer");
		when(page.getCreator()).thenReturn("alice");
		when(page.getCreated()).thenReturn(LocalDateTime.of(2024, 5, 13, 16, 3));
		when(page.getPublished()).thenReturn(LocalDateTime.of(2024, 5, 15, 12, 0));
		when(page.getEmbeds()).thenReturn("""
												  [{"type":"gallery","embed_id":12,"unknown_field":true},{"type":"collapse","items":[{"title":"A","body":"<p>a</p>"}]}]
												  """);
		SubjectResponse tag = SubjectResponse.builder()
											 .id(1L)
											 .subject("travel")
											 .build();
		when(subjects.forPages(List.of(3L))).thenReturn(Map.of(3L, List.of(tag)));

		WebSitePageResponse response = mapper.page(page);

		assertEquals("<p>safe</p>", response.getBody());
		assertEquals("body { color: red; }", response.getPageStyle());
		assertEquals("alice", response.getCreator());
		assertEquals(LocalDateTime.of(2024, 5, 13, 16, 3), response.getCreated());
		assertEquals(LocalDateTime.of(2024, 5, 15, 12, 0), response.getPublished());
		assertTrue(response.isSecure());
		assertEquals(2L, response.getAclId());
		assertEquals(2, response.getEmbeds()
								.size());
		assertEquals("gallery", response.getEmbeds()
										.get(0)
										.getType());
		assertEquals(12L, response.getEmbeds()
								  .get(0)
								  .getEmbedId());
		assertEquals("A", response.getEmbeds()
								  .get(1)
								  .getItems()
								  .get(0)
								  .getTitle());
		assertEquals(List.of(tag), response.getSubjects());
	}

	@Test
	void sourceBodyIsUsedWhenTheCacheIsEmpty() {
		WebSitePage page = mock(WebSitePage.class);
		when(page.getId()).thenReturn(4L);
		when(page.getCache()).thenReturn("");
		when(page.getBody()).thenReturn("source");

		assertEquals("source", mapper.page(page)
									 .getBody());
	}

	@Test
	void summariesOmitBodyAndAuditFieldsButKeepListFields() {
		WebSitePage page = mock(WebSitePage.class);
		when(page.getId()).thenReturn(5L);
		when(page.getTitle()).thenReturn("Title");
		when(page.getFilePath()).thenReturn("a/b");
		when(page.getPublished()).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
		when(subjects.forPages(List.of(5L))).thenReturn(Map.of());

		WebSitePageResponse summary = mapper.pageSummaries(List.of(page))
											.get(0);

		assertEquals("Title", summary.getTitle());
		assertEquals("a/b", summary.getFilePath());
		assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), summary.getPublished());
		assertNull(summary.getBody());
		assertNull(summary.getCreator());
		assertNull(summary.getCreated());
		assertEquals(List.of(), summary.getSubjects());
	}

	@Test
	void blankAndMalformedEmbedsYieldNull() {
		assertNull(mapper.embeds(null));
		assertNull(mapper.embeds("  "));
		assertNull(mapper.embeds("{not json"));
		assertNull(mapper.embeds("{\"type\":\"object-not-array\"}"));
	}

	@Test
	void fileLocationIsOnlyResolvedForAuthenticatedCallers() {
		WebSiteFile file = mock(WebSiteFile.class);
		when(file.getId()).thenReturn(9L);
		when(file.getFileId()).thenReturn(90L);
		when(file.getFilePath()).thenReturn("images/a.jpg");
		when(file.getThumbnailPath()).thenReturn("images/.thumb/a.jpg");
		when(file.getOriginalDateTime()).thenReturn(OffsetDateTime.parse("2024-05-13T16:03:44Z"));
		when(file.getRightsHolder()).thenReturn("Alice");
		when(file.getCreatorName()).thenReturn("Alice Example");
		when(file.getLocationId()).thenReturn(42L);
		when(subjects.forFiles(List.of(9L))).thenReturn(Map.of());

		when(currentUser.current()).thenReturn(Optional.empty());
		WebSiteFileResponse anonymous = mapper.file(file);

		assertEquals(90L, anonymous.getFileId());
		assertEquals("images/.thumb/a.jpg", anonymous.getThumbnailPath());
		assertEquals(OffsetDateTime.parse("2024-05-13T16:03:44Z"), anonymous.getOriginalDateTime());
		assertEquals("Alice", anonymous.getRightsHolder());
		assertEquals("Alice Example", anonymous.getCreatorName());
		assertEquals(42L, anonymous.getLocationId());
		assertNull(anonymous.getLocation());
		verifyNoInteractions(locations);

		WebGpsLocation location = mock(WebGpsLocation.class);
		when(location.getId()).thenReturn(42L);
		when(location.getLatitude()).thenReturn(new BigDecimal("60.16952"));
		when(location.getLatitudeRef()).thenReturn("N");
		when(location.getCity()).thenReturn("Helsinki");
		when(locations.findByIdIn(Set.of(42L))).thenReturn(List.of(location));
		when(currentUser.current()).thenReturn(Optional.of(new AuthenticatedUser(1, "alice", false, "t")));

		WebSiteFileResponse authenticated = mapper.file(file);

		assertEquals(42L, authenticated.getLocation()
									   .getId());
		assertEquals(new BigDecimal("60.16952"), authenticated.getLocation()
															  .getLatitude());
		assertEquals("Helsinki", authenticated.getLocation()
											  .getCity());
	}

	@Test
	void filesWithoutLocationDoNotQueryLocations() {
		WebSiteFile file = mock(WebSiteFile.class);
		when(file.getId()).thenReturn(1L);
		when(file.getLocationId()).thenReturn(null);
		when(currentUser.current()).thenReturn(Optional.of(new AuthenticatedUser(1, "alice", false, "t")));
		when(subjects.forFiles(List.of(1L))).thenReturn(Map.of());

		assertNull(mapper.files(List.of(file))
						 .get(0)
						 .getLocation());
		verifyNoInteractions(locations);
	}

	@Test
	void galleriesCarryBulkResolvedSubjects() {
		WebSiteGallery gallery = mock(WebSiteGallery.class);
		when(gallery.getId()).thenReturn(2L);
		when(gallery.getGalleryId()).thenReturn(1050L);
		when(gallery.getShortname()).thenReturn("Summer");
		when(gallery.getAclId()).thenReturn(7L);
		SubjectResponse tag = SubjectResponse.builder()
											 .id(1L)
											 .subject("summer")
											 .build();
		when(subjects.forGalleries(List.of(2L))).thenReturn(Map.of(2L, List.of(tag)));

		var response = mapper.gallery(gallery);

		assertEquals(1050L, response.getGalleryId());
		assertEquals("Summer", response.getShortname());
		assertEquals(7L, response.getAclId());
		assertEquals(List.of(tag), response.getSubjects());
	}
}
