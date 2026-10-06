package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.repository.WebGpsLocationRepository;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubjectSearchServiceUTC {
	private final WebSitePageRepository    pages       = mock(WebSitePageRepository.class);
	private final WebSiteGalleryRepository galleries   = mock(WebSiteGalleryRepository.class);
	private final WebSiteFileRepository    files       = mock(WebSiteFileRepository.class);
	private final SubjectLookupService     subjects    = mock(SubjectLookupService.class);
	private final CurrentUserProvider      currentUser = mock(CurrentUserProvider.class);
	private final ResponseMapperService    mapper      = new ResponseMapperService(subjects, mock(WebGpsLocationRepository.class), currentUser,
																				   new ObjectMapper());
	private final SubjectSearchService     service     = new SubjectSearchService(pages, galleries, files, mapper);

	@Test
	void emptySubjectListReturnsEmptyPagedSections() {
		var response = service.search(List.of(), 3, 100, -1);

		assertEquals(3, response.getPages()
								.getPage());
		assertEquals(50, response.getPages()
								 .getSize());
		assertEquals(0, response.getPages()
								.getTotalElements());
		assertEquals(0, response.getGalleries()
								.getTotalElements());
		assertEquals(0, response.getFiles()
								.getTotalElements());
	}

	@Test
	void mapsAndPaginatesAllResourceTypesAsTypedResponses() {
		WebSitePage page = mock(WebSitePage.class);
		when(page.getId()).thenReturn(1L);
		when(page.getPageId()).thenReturn(11L);
		when(page.getTitle()).thenReturn("Page");
		when(page.getHeader()).thenReturn("Header");
		when(page.getFilePath()).thenReturn("page");

		WebSiteGallery gallery = mock(WebSiteGallery.class);
		when(gallery.getId()).thenReturn(2L);
		when(gallery.getGalleryId()).thenReturn(22L);
		when(gallery.getShortname()).thenReturn("Gallery");
		when(gallery.getDescription()).thenReturn("Description");

		WebSiteFile file = mock(WebSiteFile.class);
		when(file.getId()).thenReturn(3L);
		when(file.getFileId()).thenReturn(33L);
		when(file.getFilePath()).thenReturn("file.jpg");
		when(file.getMimetype()).thenReturn("image/jpeg");
		when(file.getLocationId()).thenReturn(5L);

		when(currentUser.current()).thenReturn(Optional.empty());
		when(pages.findBySubjectIdsForUser(List.of(7L), 5, 10, 10)).thenReturn(List.of(page));
		when(galleries.findBySubjectIdsForUser(List.of(7L), 5, 10, 10)).thenReturn(List.of(gallery));
		when(files.findBySubjectIdsForUser(List.of(7L), 5, 10, 10)).thenReturn(List.of(file));
		when(pages.countBySubjectIdsForUser(List.of(7L), 5)).thenReturn(11L);
		when(galleries.countBySubjectIdsForUser(List.of(7L), 5)).thenReturn(1L);
		when(files.countBySubjectIdsForUser(List.of(7L), 5)).thenReturn(21L);
		when(subjects.forPages(List.of(1L))).thenReturn(Map.of());
		when(subjects.forGalleries(List.of(2L))).thenReturn(Map.of());
		when(subjects.forFiles(List.of(3L))).thenReturn(Map.of());

		var response = service.search(List.of(7L), 1, 10, 5);

		var pageResponse = response.getPages()
								   .getContent()
								   .get(0);
		assertEquals("Page", pageResponse.getTitle());
		assertEquals(11L, pageResponse.getPageId());
		assertNull(pageResponse.getBody(), "search results are summaries without body");
		assertEquals(List.of(), pageResponse.getSubjects());
		assertEquals("Gallery", response.getGalleries()
										.getContent()
		                                .get(0)
										.getShortname());
		var fileResponse = response.getFiles()
								   .getContent()
								   .get(0);
		assertEquals("file.jpg", fileResponse.getFilePath());
		assertEquals(5L, fileResponse.getLocationId());
		assertNull(fileResponse.getLocation(), "anonymous callers never receive GPS locations");
		assertEquals(11L, response.getPages()
								  .getTotalElements());
		assertEquals(2, response.getPages()
								.getTotalPages());
	}
}
