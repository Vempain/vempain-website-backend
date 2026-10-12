package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.api.response.OpenGraphResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebSiteConfiguration;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.repository.WebGpsLocationRepository;
import fi.poltsi.vempain.website.repository.WebSiteConfigurationRepository;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenGraphServiceUTC {
	private final WebSitePageRepository          pages         = mock(WebSitePageRepository.class);
	private final WebSiteGalleryRepository       galleries     = mock(WebSiteGalleryRepository.class);
	private final WebSiteFileRepository          files         = mock(WebSiteFileRepository.class);
	private final WebSiteConfigurationRepository configuration = mock(WebSiteConfigurationRepository.class);
	private final ResourceAccessService          access        = mock(ResourceAccessService.class);
	private final CurrentUserProvider            currentUser   = mock(CurrentUserProvider.class);
	private final ResponseMapperService          mapper        = new ResponseMapperService(mock(SubjectLookupService.class),
																						   mock(WebGpsLocationRepository.class), currentUser,
																						   new ObjectMapper());
	private final OpenGraphService               service       = new OpenGraphService(pages, galleries, files, configuration, mapper, access,
																					  currentUser);

	@BeforeEach
	void setUp() {
		when(configuration.findByConfigKey(anyString())).thenReturn(Optional.empty());
		configure("site.name", "My Site", "Default name");
		configure("site.description", "", "Default description");
		when(access.isAccessible(null)).thenReturn(true);
		when(currentUser.currentUserId()).thenReturn(-1L);
	}

	@Test
	void pageUsesTheHeaderAsDescriptionAndTheFirstAccessibleImageEmbed() {
		WebSitePage page = page("trips/summer", "Summer trip", "Photos from the trip", "<p>Body</p>", """
				[{"type":"collapse"},{"type":"video","embed_id":4},{"type":"image","embed_id":5},{"type":"image","embed_id":6}]
				""");
		when(page.getPublished()).thenReturn(LocalDateTime.of(2024, 5, 15, 12, 0));
		when(page.getModified()).thenReturn(LocalDateTime.of(2024, 5, 16, 9, 30));
		WebSiteFile beach = file("trips/summer/beach.jpg", "image/jpeg", 1400L, 933L, null);
		when(files.findByFileId(5L)).thenReturn(Optional.of(beach));

		OpenGraphResponse meta = service.forPage(page);

		assertEquals("article", meta.getType());
		assertEquals("My Site", meta.getSiteName());
		assertEquals("Summer trip", meta.getTitle());
		assertEquals("Photos from the trip", meta.getDescription());
		assertEquals("/pages/trips/summer", meta.getPath());
		assertEquals("trips/summer/beach.jpg", meta.getImagePath());
		assertEquals("image/jpeg", meta.getImageMimetype());
		assertEquals(1400L, meta.getImageWidth());
		assertEquals(933L, meta.getImageHeight());
		assertEquals(LocalDateTime.of(2024, 5, 15, 12, 0), meta.getPublished());
		assertEquals(LocalDateTime.of(2024, 5, 16, 9, 30), meta.getModified());
		verify(files, never()).findByFileId(4L);
		verify(files, never()).findByFileId(6L);
	}

	@Test
	void pageWithoutHeaderSummarizesTheBodyAndUsesTheFirstGalleryImageFromTheBody() {
		String      text  = "Word ".repeat(60);
		WebSitePage page  = page("trips/winter", "Winter", "  ", "<!--vps:embed:gallery:7--><h1>Title &amp; more</h1><p>" + text + "</p>", null);
		WebSiteFile clip  = file("g/clip.mp4", "video/mp4", null, null, null);
		WebSiteFile photo = file("g/a.jpg", "image/jpeg", 800L, 600L, null);
		when(files.findByGalleryIdForUser(7L, -1L)).thenReturn(List.of(clip, photo));

		OpenGraphResponse meta = service.forPage(page);

		assertTrue(meta.getDescription()
					   .startsWith("Title & more Word Word"), meta.getDescription());
		assertTrue(meta.getDescription()
					   .endsWith("…"));
		assertTrue(meta.getDescription()
					   .length() <= OpenGraphService.DESCRIPTION_LENGTH + 1);
		assertFalse(meta.getDescription()
						.contains("<"));
		assertEquals("g/a.jpg", meta.getImagePath());
		assertEquals("image/jpeg", meta.getImageMimetype());
	}

	@Test
	void pageSkipsFilesTheCallerMayNotSeeAndVideoHeroes() {
		WebSitePage page = page("trips/secret", "Secret", "Header", "<p>Body</p>", """
				[{"type":"image","embed_id":1},{"type":"hero","embed_id":2,"hero_type":"video"},{"type":"hero","embed_id":3,"hero_type":"carousel"}]
				""");
		when(currentUser.currentUserId()).thenReturn(42L);
		WebSiteFile restricted = file("p/private.jpg", "image/jpeg", 10L, 10L, 9L);
		WebSiteFile first      = file("c/first.png", "image/png", 300L, 200L, null);
		when(files.findByFileId(1L)).thenReturn(Optional.of(restricted));
		when(access.isAccessible(9L)).thenReturn(false);
		when(files.findByGalleryIdForUser(3L, 42L)).thenReturn(List.of(first));

		OpenGraphResponse meta = service.forPage(page);

		assertEquals("c/first.png", meta.getImagePath());
		verify(files, never()).findByFileId(2L);
	}

	@Test
	void pageWithoutImagesOrTextFallsBackToTheSiteDescription() {
		WebSitePage page = page("empty", "Empty", "", "<p> </p>", "[]");

		OpenGraphResponse meta = service.forPage(page);

		assertNull(meta.getImagePath());
		assertNull(meta.getImageMimetype());
		assertEquals("Default description", meta.getDescription());
		assertEquals("/pages/empty", meta.getPath());
	}

	@Test
	void routesResolvePagesAndHideRestrictedOrMissingOnesBehindTheSite() {
		WebSitePage summer = page("trips/summer", "Summer trip", "Photos", "<p>Body</p>", "[]");
		WebSitePage secret = page("secret", "Secret title", "Secret header", "<p>Secret body</p>", "[]");
		when(secret.getAclId()).thenReturn(9L);
		when(access.isAccessible(9L)).thenReturn(false);
		when(pages.findByFilePath("trips/summer")).thenReturn(Optional.of(summer));
		when(pages.findByFilePath("secret")).thenReturn(Optional.of(secret));
		when(pages.findByFilePath("index")).thenReturn(Optional.empty());

		OpenGraphResponse found = service.forRoute("/pages/trips/summer");
		assertEquals("article", found.getType());
		assertEquals("Summer trip", found.getTitle());
		assertEquals("/pages/trips/summer", found.getPath());

		OpenGraphResponse hidden = service.forRoute("pages/secret/");
		assertEquals("website", hidden.getType());
		assertEquals("My Site", hidden.getTitle());
		assertEquals("Default description", hidden.getDescription());
		assertEquals("/pages/secret", hidden.getPath());
		assertNull(hidden.getImagePath());

		assertEquals("/pages/index", service.forRoute("")
											.getPath());
		assertEquals("/pages/index", service.forRoute("/")
											.getPath());
		assertEquals("/pages/index", service.forRoute("/pages")
											.getPath());
		assertEquals("/pages/index", service.forRoute("/whatever/else")
											.getPath());
		assertEquals("/search", service.forRoute("/search")
									   .getPath());
		assertEquals("/galleries", service.forRoute("/galleries")
										  .getPath());
		assertEquals("website", service.forRoute("/search")
									   .getType());
		verify(pages, times(3)).findByFilePath("index");
	}

	@Test
	void galleryRoutesUseTheShortnameDescriptionAndFirstImage() {
		WebSiteGallery gallery = gallery(12L, "Summer", "Trip photos", null);
		when(gallery.getCreated()).thenReturn(LocalDateTime.of(2024, 5, 1, 10, 0));
		WebSiteGallery unnamedGallery = gallery(13L, null, null, null);
		WebSiteGallery privateGallery = gallery(14L, "Private", "Hidden", 5L);
		WebSiteFile    cover          = file("s/1.png", "image/png", 800L, 600L, null);
		when(galleries.findByGalleryId(12L)).thenReturn(Optional.of(gallery));
		when(galleries.findByGalleryId(13L)).thenReturn(Optional.of(unnamedGallery));
		when(galleries.findByGalleryId(14L)).thenReturn(Optional.of(privateGallery));
		when(access.isAccessible(5L)).thenReturn(false);
		when(galleries.findByGalleryId(99L)).thenReturn(Optional.empty());
		when(files.findByGalleryIdForUser(anyLong(), anyLong())).thenReturn(List.of());
		when(files.findByGalleryIdForUser(12L, -1L)).thenReturn(List.of(cover));

		OpenGraphResponse meta = service.forRoute("/galleries/12");
		assertEquals("website", meta.getType());
		assertEquals("Summer", meta.getTitle());
		assertEquals("Trip photos", meta.getDescription());
		assertEquals("/galleries/12", meta.getPath());
		assertEquals("s/1.png", meta.getImagePath());
		assertEquals(LocalDateTime.of(2024, 5, 1, 10, 0), meta.getPublished());

		OpenGraphResponse unnamed = service.forRoute("/galleries/13");
		assertEquals("Gallery #13", unnamed.getTitle());
		assertEquals("Default description", unnamed.getDescription());
		assertNull(unnamed.getImagePath());

		OpenGraphResponse hidden = service.forRoute("/galleries/14");
		assertEquals("My Site", hidden.getTitle());
		assertEquals("Default description", hidden.getDescription());
		assertEquals("/galleries/14", hidden.getPath());

		assertEquals("My Site", service.forRoute("/galleries/99")
									   .getTitle());
	}

	@Test
	void siteTextsFallBackToDefaultsWithoutConfiguration() {
		when(configuration.findByConfigKey(anyString())).thenReturn(Optional.empty());

		OpenGraphResponse meta = service.forRoute("/search");

		assertEquals(OpenGraphService.DEFAULT_SITE_NAME, meta.getSiteName());
		assertEquals(OpenGraphService.DEFAULT_SITE_NAME, meta.getTitle());
		assertNull(meta.getDescription());
	}

	@Test
	void summarizeStripsMarkupDecodesEntitiesAndTruncatesAtAWordBoundary() {
		assertNull(OpenGraphService.summarize(null));
		assertNull(OpenGraphService.summarize("<p>  </p><!-- comment -->"));
		assertEquals("Hi & bye", OpenGraphService.summarize("<!--vps:embed:image:1--><b>Hi</b>\n &amp;   <i>bye</i>"));

		String summary = OpenGraphService.summarize("<p>" + "abcdefghij ".repeat(40) + "</p>");
		assertTrue(summary.endsWith("…"));
		assertTrue(summary.length() <= OpenGraphService.DESCRIPTION_LENGTH + 1);
		assertFalse(summary.endsWith(" …"));
		assertTrue(summary.chars()
						  .filter(c -> c == ' ')
						  .count() > 10);
	}

	private void configure(String key, String value, String defaultValue) {
		WebSiteConfiguration entry = mock(WebSiteConfiguration.class);
		when(entry.getConfigValue()).thenReturn(value);
		when(entry.getConfigDefault()).thenReturn(defaultValue);
		when(configuration.findByConfigKey(key)).thenReturn(Optional.of(entry));
	}

	private static WebSitePage page(String filePath, String title, String header, String body, String embeds) {
		WebSitePage page = mock(WebSitePage.class);
		when(page.getFilePath()).thenReturn(filePath);
		when(page.getTitle()).thenReturn(title);
		when(page.getHeader()).thenReturn(header);
		when(page.getBody()).thenReturn(body);
		when(page.getCache()).thenReturn(null);
		when(page.getEmbeds()).thenReturn(embeds);
		when(page.getAclId()).thenReturn(null);
		return page;
	}

	private static WebSiteFile file(String path, String mimetype, Long width, Long height, Long aclId) {
		WebSiteFile file = mock(WebSiteFile.class);
		when(file.getFilePath()).thenReturn(path);
		when(file.getMimetype()).thenReturn(mimetype);
		when(file.getWidth()).thenReturn(width);
		when(file.getHeight()).thenReturn(height);
		when(file.getAclId()).thenReturn(aclId);
		return file;
	}

	private static WebSiteGallery gallery(long galleryId, String shortname, String description, Long aclId) {
		WebSiteGallery gallery = mock(WebSiteGallery.class);
		when(gallery.getGalleryId()).thenReturn(galleryId);
		when(gallery.getShortname()).thenReturn(shortname);
		when(gallery.getDescription()).thenReturn(description);
		when(gallery.getAclId()).thenReturn(aclId);
		return gallery;
	}
}
