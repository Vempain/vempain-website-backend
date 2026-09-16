package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.service.ResourceAccessService;
import fi.poltsi.vempain.website.service.SubjectLookupService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceControllerUTC {
	private final WebSiteFileRepository files = mock(WebSiteFileRepository.class);
	private final WebSiteGalleryRepository galleries = mock(WebSiteGalleryRepository.class);
	private final SubjectLookupService subjects = mock(SubjectLookupService.class);
	private final ResourceAccessService access = mock(ResourceAccessService.class);
	private final CurrentUserProvider user = mock(CurrentUserProvider.class);
	private final SiteProperties properties = new SiteProperties();
	private final ResourceController controller =
			new ResourceController(files, galleries, subjects, access, user, properties);

	@Test
	void publicApiPathStreamsFileContent(@TempDir Path root) throws Exception {
		Path file = Files.createDirectories(root.resolve("document/site"))
		                    .resolve("default-style.json");
		Files.writeString(file, "{\"theme\":\"default\"}");
		properties.setFilesRoot(root.toString());
		WebSiteFile entity = fileEntity("document/site/default-style.json", "application/json", null);
		when(user.currentUserId()).thenReturn(-1L);
		when(files.findByFilePath(entity.getFilePath())).thenReturn(Optional.of(entity));
		HttpServletRequest request = request(null);

		ResponseEntity<Resource> response = controller.raw(entity.getFilePath(), request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("application/json", response.getHeaders().getFirst("Content-Type"));
		assertEquals("{\"theme\":\"default\"}", new String(response.getBody().getInputStream().readAllBytes()));
		verify(access).requireAccess(null);
	}

	@Test
	void normalizesLeadingSlashFromCatchAllRoute(@TempDir Path root) throws Exception {
		Path file = Files.createDirectories(root.resolve("document/site"))
		                    .resolve("default-style.json");
		Files.writeString(file, "{\"theme\":\"default\"}");
		properties.setFilesRoot(root.toString());
		WebSiteFile entity = fileEntity("document/site/default-style.json", "application/json", 1L);
		when(files.findByFilePath(entity.getFilePath())).thenReturn(Optional.of(entity));

		ResponseEntity<Resource> response = controller.raw("/document/site/default-style.json", request(null));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("{\"theme\":\"default\"}", new String(response.getBody().getInputStream().readAllBytes()));
		verify(access).requireAccess(1L);
	}

	@Test
	void findsPublicFileByFileIdNotDatabasePrimaryKey() {
		WebSiteFile entity = fileEntity("image/photo.jpg", "image/jpeg", 66812L);
		when(entity.getFileId()).thenReturn(72758L);
		when(files.findByFileId(72758L)).thenReturn(Optional.of(entity));

		Map<?, ?> response = (Map<?, ?>) controller.fileById(72758L);

		assertEquals(72758L, response.get("file_id"));
		assertEquals("image/photo.jpg", response.get("file_path"));
		verify(files).findByFileId(72758L);
		verify(access).requireAccess(66812L);
		verify(files, never()).findById(72758L);
	}

	@Test
	void exposesPublicGalleryFilesRouteForPageEmbeds() throws Exception {
		GetMapping mapping = ResourceApi.class
				.getMethod("galleryFiles", long.class, int.class, int.class)
				.getAnnotation(GetMapping.class);

		assertTrue(Arrays.asList(mapping.path()).contains("/api/public/galleries/{galleryId}/files"));
	}

	@Test
	void requestsGalleryFilesUsingExternalGalleryId() {
		when(user.currentUserId()).thenReturn(-1L);
		when(files.findByGalleryIdForUser(1050L, -1L)).thenReturn(java.util.List.of());

		controller.galleryFiles(1050L, 0, 25);

		verify(files).findByGalleryIdForUser(1050L, -1L);
	}

	@Test
	void supportsRangesAndThumbnailMetadataFallback(@TempDir Path root) throws Exception {
		Path file = Files.createDirectories(root.resolve("images")).resolve("photo.jpg");
		Files.writeString(file, "0123456789");
		Files.createDirectories(root.resolve("images/.thumb"));
		Files.writeString(root.resolve("images/.thumb/photo.jpg"), "0123456789");
		properties.setFilesRoot(root.toString());
		WebSiteFile entity = fileEntity("images/photo.jpg", "image/jpeg", 7L);
		when(user.currentUserId()).thenReturn(2L);
		when(files.findByFilePath("images/.thumb/photo.jpg")).thenReturn(Optional.empty());
		when(files.findByFilePath("images/photo.jpg")).thenReturn(Optional.of(entity));

		ResponseEntity<Resource> response = controller.raw(
				"images/.thumb/photo.jpg", request("bytes=2-5"));

		assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode());
		assertEquals("bytes 2-5/10", response.getHeaders().getFirst("Content-Range"));
		assertEquals("2345", new String(response.getBody().getInputStream().readAllBytes()));
		verify(access).requireAccess(7L);
	}

	@Test
	void rejectsMissingFilesAndTraversal(@TempDir Path root) throws Exception {
		properties.setFilesRoot(root.toString());
		WebSiteFile entity = fileEntity("../secret.txt", "text/plain", null);
		when(user.currentUserId()).thenReturn(-1L);
		when(files.findByFilePath("../secret.txt")).thenReturn(Optional.of(entity));

		ResponseEntity<Resource> response = controller.raw("../secret.txt", request(null));

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	void rejectsSymlinkEscapingTheConfiguredRoot(@TempDir Path root) throws Exception {
		Path outside   = Files.writeString(root.resolveSibling("outside.txt"), "secret");
		Path filesRoot = Files.createDirectory(root.resolve("files"));
		Files.createSymbolicLink(filesRoot.resolve("linked.txt"), outside);
		properties.setFilesRoot(filesRoot.toString());
		WebSiteFile entity = fileEntity("linked.txt", "text/plain", null);
		when(files.findByFilePath("linked.txt")).thenReturn(Optional.of(entity));

		ResponseEntity<Resource> response = controller.raw("linked.txt", request(null));

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	void returnsForbiddenWhenPathHasNoVisibleMetadata() throws Exception {
		when(user.currentUserId()).thenReturn(-1L);
		when(files.findByFilePath("missing.txt")).thenReturn(Optional.empty());

		ResponseEntity<Resource> response = controller.raw("missing.txt", request(null));

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		verifyNoInteractions(access);
	}

	@Test
	void returnsUnsatisfiableForInvalidRange(@TempDir Path root) throws Exception {
		Path file = Files.writeString(root.resolve("file.txt"), "content");
		properties.setFilesRoot(root.toString());
		WebSiteFile entity = fileEntity("file.txt", "text/plain", null);
		when(user.currentUserId()).thenReturn(-1L);
		when(files.findByFilePath("file.txt")).thenReturn(Optional.of(entity));

		ResponseEntity<Resource> response = controller.raw("file.txt", request("bytes=99-100"));

		assertEquals(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, response.getStatusCode());
		assertEquals("bytes", response.getHeaders().getFirst("Accept-Ranges"));
		assertTrue(Files.exists(file));
	}

	private WebSiteFile fileEntity(String path, String mimetype, Long aclId) {
		WebSiteFile entity = mock(WebSiteFile.class);
		when(entity.getFilePath()).thenReturn(path);
		when(entity.getMimetype()).thenReturn(mimetype);
		when(entity.getAclId()).thenReturn(aclId);
		return entity;
	}

	private HttpServletRequest request(String range) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Range")).thenReturn(range);
		return request;
	}
}
