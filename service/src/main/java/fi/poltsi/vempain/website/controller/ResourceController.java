package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.WebSiteFileResponse;
import fi.poltsi.vempain.website.api.response.WebSiteGalleryResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.service.ResourceAccessService;
import fi.poltsi.vempain.website.service.ResponseMapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResourceController implements ResourceApi {
	private final WebSiteFileRepository    files;
	private final WebSiteGalleryRepository galleries;
	private final ResponseMapperService    mapper;
	private final ResourceAccessService    access;
	private final CurrentUserProvider      user;
	private final SiteProperties           props;

	public PagedResponse<WebSiteFileResponse> listFiles(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int perPage) {
		List<WebSiteFile> all  = files.findAllFilesForUser(user.currentUserId());
		int               n    = Math.max(1, Math.min(50, perPage)), p = Math.max(0, page);
		int               from = Math.min(all.size(), p * n);
		return PagedResponse.of(mapper.files(all.subList(from, Math.min(all.size(), from + n))), p, n, all.size());
	}

	public WebSiteFileResponse fileById(@PathVariable long id) {
		WebSiteFile file = files.findByFileId(id)
								.orElseThrow(() -> ApiException.notFound("File not found"));
		access.requireAccess(file.getAclId());
		return mapper.file(file);
	}

	public List<WebSiteGalleryResponse> galleries() {
		return mapper.galleries(galleries.findAll());
	}

	public List<WebSiteGalleryResponse> publicGalleries() {
		return mapper.galleries(galleries.findAllAccessible(user.currentUserId()));
	}

	public PagedResponse<WebSiteFileResponse> galleryFiles(@PathVariable long galleryId, @RequestParam(defaultValue = "0") int page,
														   @RequestParam(defaultValue = "25") int perPage) {
		int               p    = Math.max(0, page);
		int               n    = Math.max(1, Math.min(50, perPage));
		List<WebSiteFile> all  = files.findByGalleryIdForUser(galleryId, user.currentUserId());
		int               from = Math.min(all.size(), p * n);
		return PagedResponse.of(mapper.files(all.subList(from, Math.min(all.size(), from + n))), p, n, all.size());
	}

	/**
	 * Streams file bytes from both public file URL shapes used by the PHP backend.
	 * The API path is intentionally content-serving; metadata is available from the list
	 * and ID endpoints.
	 */
	public ResponseEntity<Resource> raw(@PathVariable String path, jakarta.servlet.http.HttpServletRequest request)
			throws java.io.IOException {
		String relativePath = path.replaceFirst("^/+", "");
		WebSiteFile entity = files.findByFilePath(relativePath).orElse(null);
		if (entity == null && relativePath.contains("/.thumb/")) {
			String lookup = relativePath.replaceFirst("/\\.thumb/", "/");
			entity = files.findByFilePath(lookup).orElse(null);
		}
		if (entity == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		access.requireAccess(entity.getAclId());
		Path root = Paths.get(props.getFilesRoot()).toAbsolutePath().normalize();
		if (!Files.isDirectory(root)) {
			return ResponseEntity.notFound()
			                     .build();
		}
		Path realRoot = root.toRealPath();
		Path target = root.resolve(relativePath).normalize();
		if (!target.startsWith(root) || !Files.isRegularFile(target)) {
			return ResponseEntity.notFound().build();
		}
		Path realTarget = target.toRealPath();
		if (!realTarget.startsWith(realRoot) || !Files.isRegularFile(realTarget)) {
			return ResponseEntity.notFound()
			                     .build();
		}
		target = realTarget;

		long length = Files.size(target);
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept-Ranges", "bytes");
		headers.setContentType(MediaType.parseMediaType(entity.getMimetype()));
		String range = request.getHeader("Range");
		if (range != null && range.matches("bytes=\\d*-\\d*")) {
			String[] bounds = range.substring(6).split("-", 2);
			if (bounds[0].isEmpty() && bounds[1].isEmpty()) {
				return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).headers(headers).build();
			}
			long start = bounds[0].isEmpty() ? Math.max(0, length - Long.parseLong(bounds[1])) : Long.parseLong(bounds[0]);
			long end = bounds[1].isEmpty() ? length - 1 : Long.parseLong(bounds[1]);
			if (start < 0 || start > end || end >= length) {
				return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).headers(headers).build();
			}
			headers.setContentLength(end - start + 1);
			headers.set("Content-Range", "bytes " + start + "-" + end + "/" + length);
			java.io.InputStream input = Files.newInputStream(target);
			input.skipNBytes(start);
			long remaining = end - start + 1;
			input = new java.io.FilterInputStream(input) {
				private long bytesRemaining = remaining;

				@Override
				public int read() throws java.io.IOException {
					if (bytesRemaining == 0) {
						return -1;
					}
					int value = super.read();
					if (value >= 0) {
						bytesRemaining--;
					}
					return value;
				}

				@Override
				public int read(byte[] bytes, int offset, int count) throws java.io.IOException {
					if (bytesRemaining == 0) {
						return -1;
					}
					int read = super.read(bytes, offset, (int) Math.min(count, bytesRemaining));
					if (read > 0) {
						bytesRemaining -= read;
					}
					return read;
				}
			};
			return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
			                     .headers(headers)
			                     .body(new InputStreamResource(input));
		}
		headers.setContentLength(length);
		return ResponseEntity.ok().headers(headers).body(new FileSystemResource(target));
	}
}
