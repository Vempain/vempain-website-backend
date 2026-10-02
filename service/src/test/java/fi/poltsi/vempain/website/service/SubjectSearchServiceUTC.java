package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubjectSearchServiceUTC {
	private final WebSitePageRepository    pages     = mock(WebSitePageRepository.class);
	private final WebSiteGalleryRepository galleries = mock(WebSiteGalleryRepository.class);
	private final WebSiteFileRepository    files     = mock(WebSiteFileRepository.class);
	private final SubjectLookupService     subjects  = mock(SubjectLookupService.class);
	private final SubjectSearchService     service   = new SubjectSearchService(pages, galleries, files, subjects);

	@Test
	void emptySubjectListReturnsEmptyPagedSections() {
		var response = service.search(List.of(), 3, 100, -1);

		assertEquals(3, response.pages()
		                        .page());
		assertEquals(50, response.pages()
		                         .size());
		assertEquals(0, response.pages()
		                        .totalElements());
		assertEquals(0, response.galleries()
		                        .totalElements());
		assertEquals(0, response.files()
		                        .totalElements());
	}

	@Test
	void mapsAndPaginatesAllResourceTypes() {
		WebSitePage page = mock(WebSitePage.class);
		when(page.getId()).thenReturn(1L);
		when(page.getPageId()).thenReturn(11L);
		when(page.getTitle()).thenReturn("Page");
		when(page.getHeader()).thenReturn("Header");
		when(page.getFilePath()).thenReturn("page");
		when(page.getPublished()).thenReturn(null);
		when(page.getAclId()).thenReturn(null);

		WebSiteGallery gallery = mock(WebSiteGallery.class);
		when(gallery.getId()).thenReturn(2L);
		when(gallery.getGalleryId()).thenReturn(22L);
		when(gallery.getShortname()).thenReturn("Gallery");
		when(gallery.getDescription()).thenReturn("Description");
		when(gallery.getAclId()).thenReturn(null);

		WebSiteFile file = mock(WebSiteFile.class);
		when(file.getId()).thenReturn(3L);
		when(file.getFileId()).thenReturn(33L);
		when(file.getFilePath()).thenReturn("file.jpg");
		when(file.getMimetype()).thenReturn("image/jpeg");
		when(file.getAclId()).thenReturn(null);

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

		assertEquals("Page", response.pages()
		                             .content()
		                             .get(0)
		                             .get("title"));
		assertEquals("Gallery", response.galleries()
		                                .content()
		                                .get(0)
		                                .get("shortname"));
		assertEquals("file.jpg", response.files()
		                                 .content()
		                                 .get(0)
		                                 .get("file_path"));
		assertEquals(11L, response.pages()
		                          .totalElements());
		assertEquals(2, response.pages()
		                        .totalPages());
	}
}
