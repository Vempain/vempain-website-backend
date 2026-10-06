package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.api.response.LastItemsResponse;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LastItemsServiceUTC {
	private final WebSitePageRepository    pages     = mock(WebSitePageRepository.class);
	private final WebSiteFileRepository    files     = mock(WebSiteFileRepository.class);
	private final WebSiteGalleryRepository galleries = mock(WebSiteGalleryRepository.class);
	private final LastItemsService         service   = new LastItemsService(pages, files, galleries);

	@Test
	void pagesUseTheRenderedBodyAndClampTheCount() {
		WebSitePage page = mock(WebSitePage.class);
		when(page.getId()).thenReturn(1L);
		when(page.getTitle()).thenReturn("Title");
		when(page.getHeader()).thenReturn("Header");
		when(page.getCache()).thenReturn("<p>cache</p>");
		when(page.getFilePath()).thenReturn("trips/summer");
		when(page.getPublished()).thenReturn(LocalDateTime.of(2024, 5, 15, 12, 0));
		when(pages.findLatestAccessible(eq(7L), any(Limit.class))).thenReturn(List.of(page));

		LastItemsResponse response = service.latest("Pages", 500, 7L);

		assertEquals("pages", response.getType());
		assertEquals(50, response.getCount());
		var item = response.getItems()
						   .get(0);
		assertEquals("<p>cache</p>", item.getBody());
		assertEquals("Header", item.getHeader());
		assertEquals("trips/summer", item.getFilePath());
		assertEquals(LocalDateTime.of(2024, 5, 15, 12, 0), item.getPublished());
		verify(pages).findLatestAccessible(7L, Limit.of(50));
	}

	@Test
	void galleriesFallBackToAGeneratedTitle() {
		WebSiteGallery named = mock(WebSiteGallery.class), unnamed = mock(WebSiteGallery.class);
		when(named.getId()).thenReturn(1L);
		when(named.getGalleryId()).thenReturn(10L);
		when(named.getShortname()).thenReturn("Summer");
		when(unnamed.getId()).thenReturn(2L);
		when(unnamed.getGalleryId()).thenReturn(20L);
		when(unnamed.getShortname()).thenReturn("");
		when(galleries.findLatestAccessible(eq(-1L), any(Limit.class))).thenReturn(List.of(named, unnamed));

		var items = service.latest("galleries", 0, -1L)
						   .getItems();

		assertEquals("Summer", items.get(0)
									.getTitle());
		assertEquals(10L, items.get(0)
							   .getGalleryId());
		assertEquals("Gallery #20", items.get(1)
										 .getTitle());
		assertNull(items.get(1)
						.getPublished());
		verify(galleries).findLatestAccessible(-1L, Limit.of(1));
	}

	@Test
	void filesUseTheFileNameAsTitleAndTheOriginalDateAsPublished() {
		WebSiteFile file = mock(WebSiteFile.class);
		when(file.getId()).thenReturn(3L);
		when(file.getFilePath()).thenReturn("images/trip/sunset.jpg");
		when(file.getThumbnailPath()).thenReturn("images/trip/.thumb/sunset.jpg");
		when(file.getOriginalDateTime()).thenReturn(OffsetDateTime.parse("2024-05-13T16:03:44Z"));
		when(files.findLatestByMimePrefixForUser(eq(-1L), eq("image/%"), any(Limit.class))).thenReturn(List.of(file));
		when(files.findLatestByMimePrefixForUser(eq(-1L), eq("video/%"), any(Limit.class))).thenReturn(List.of());
		when(files.findLatestByMimePrefixForUser(eq(-1L), eq("audio/%"), any(Limit.class))).thenReturn(List.of());
		when(files.findLatestDocumentsForUser(eq(-1L), any(Limit.class))).thenReturn(List.of());

		var image = service.latest("images", 5, -1L)
						   .getItems()
						   .get(0);

		assertEquals("sunset.jpg", image.getTitle());
		assertEquals("images/trip/.thumb/sunset.jpg", image.getThumbnailPath());
		assertEquals(LocalDateTime.of(2024, 5, 13, 16, 3, 44), image.getPublished());
		assertEquals(0, service.latest("videos", 5, -1L)
							   .getItems()
							   .size());
		assertEquals(0, service.latest("audio", 5, -1L)
							   .getItems()
							   .size());
		assertEquals("documents", service.latest("documents", 5, -1L)
										 .getType());
	}

	@Test
	void unsupportedTypesAreRejected() {
		assertThrows(ApiException.class, () -> service.latest("unknown", 5, -1L));
		assertThrows(ApiException.class, () -> service.latest(null, 5, -1L));
	}
}
