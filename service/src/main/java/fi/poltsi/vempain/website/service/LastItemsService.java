package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.api.response.LastItemResponse;
import fi.poltsi.vempain.website.api.response.LastItemsResponse;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Latest published items shown by the "last" page embed.
 */
@Service
@RequiredArgsConstructor
public class LastItemsService {

	private final WebSitePageRepository    pages;
	private final WebSiteFileRepository    files;
	private final WebSiteGalleryRepository galleries;

	/**
	 * @param type   pages, galleries, images, videos, audio or documents (case insensitive)
	 * @param count  requested number of items, clamped to 1..50
	 * @param userId caller used for ACL filtering
	 * @return the newest accessible items of the type
	 */
	public LastItemsResponse latest(String type, int count, long userId) {
		int    n     = Math.max(1, Math.min(50, count));
		String kind  = type == null ? "" : type.toLowerCase(Locale.ROOT);
		Limit  limit = Limit.of(n);
		List<LastItemResponse> items = switch (kind) {
			case "pages" -> pages.findLatestAccessible(userId, limit)
								 .stream()
								 .map(LastItemsService::page)
								 .toList();
			case "galleries" -> galleries.findLatestAccessible(userId, limit)
										 .stream()
										 .map(LastItemsService::gallery)
										 .toList();
			case "images" -> files(files.findLatestByMimePrefixForUser(userId, "image/%", limit));
			case "videos" -> files(files.findLatestByMimePrefixForUser(userId, "video/%", limit));
			case "audio" -> files(files.findLatestByMimePrefixForUser(userId, "audio/%", limit));
			case "documents" -> files(files.findLatestDocumentsForUser(userId, limit));
			default -> throw ApiException.badRequest("Unsupported last-items type");
		};
		return LastItemsResponse.builder()
								.type(kind)
								.count(n)
								.items(items)
								.build();
	}

	private static List<LastItemResponse> files(List<WebSiteFile> values) {
		return values.stream()
					 .map(LastItemsService::file)
					 .toList();
	}

	private static LastItemResponse page(WebSitePage page) {
		return LastItemResponse.builder()
							   .id(page.getId())
							   .title(page.getTitle())
							   .header(page.getHeader())
							   .body(ResponseMapperService.renderedBody(page))
							   .published(page.getPublished())
							   .filePath(page.getFilePath())
							   .build();
	}

	private static LastItemResponse gallery(WebSiteGallery gallery) {
		boolean unnamed = gallery.getShortname() == null || gallery.getShortname()
																   .isEmpty();
		return LastItemResponse.builder()
							   .id(gallery.getId())
							   .galleryId(gallery.getGalleryId())
							   .title(unnamed ? "Gallery #" + gallery.getGalleryId() : gallery.getShortname())
							   .build();
	}

	private static LastItemResponse file(WebSiteFile file) {
		return LastItemResponse.builder()
							   .id(file.getId())
							   .title(Path.of(file.getFilePath())
										  .getFileName()
										  .toString())
							   .published(file.getOriginalDateTime() == null ? null : file.getOriginalDateTime()
																						  .toLocalDateTime())
							   .filePath(file.getFilePath())
							   .thumbnailPath(file.getThumbnailPath())
							   .build();
	}
}
