package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.controller.dto.response.PagedResponse;
import fi.poltsi.vempain.website.controller.dto.response.SubjectSearchResponse;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubjectSearchService {
	private final WebSitePageRepository    pageRepository;
	private final WebSiteGalleryRepository galleryRepository;
	private final WebSiteFileRepository    fileRepository;
	private final SubjectLookupService     subjects;

	public SubjectSearchResponse search(Collection<Long> subjectIds, int page, int size, long userId) {
		int p = Math.max(0, page);
		int n = Math.max(1, Math.min(50, size));
		if (subjectIds == null || subjectIds.isEmpty()) {
			return empty(p, n);
		}
		List<WebSitePage>    pages     = pageRepository.findBySubjectIdsForUser(subjectIds, userId, n, p * n);
		List<WebSiteGallery> galleries = galleryRepository.findBySubjectIdsForUser(subjectIds, userId, n, p * n);
		List<WebSiteFile>    files     = fileRepository.findBySubjectIdsForUser(subjectIds, userId, n, p * n);
		return new SubjectSearchResponse(
				pageResponse(pages, p, n, pageRepository.countBySubjectIdsForUser(subjectIds, userId)),
				galleryResponse(galleries, p, n, galleryRepository.countBySubjectIdsForUser(subjectIds, userId)),
				fileResponse(files, p, n, fileRepository.countBySubjectIdsForUser(subjectIds, userId)));
	}

	private SubjectSearchResponse empty(int page, int size) {
		return new SubjectSearchResponse(PagedResponse.of(List.of(), page, size, 0),
										 PagedResponse.of(List.of(), page, size, 0), PagedResponse.of(List.of(), page, size, 0));
	}

	private PagedResponse<Map<String, Object>> pageResponse(List<WebSitePage> values, int page, int size, long total) {
		Map<Long, List<fi.poltsi.vempain.website.controller.dto.response.SubjectResponse>> tags =
				subjects.forPages(values.stream()
				                        .map(WebSitePage::getId)
				                        .toList());
		return PagedResponse.of(values.stream()
		                              .map(value -> {
										  Map<String, Object> result = new LinkedHashMap<>();
										  result.put("id", value.getId());
										  result.put("page_id", value.getPageId());
										  result.put("title", value.getTitle());
										  result.put("header", value.getHeader());
										  result.put("file_path", value.getFilePath());
										  result.put("secure", value.isSecure());
										  result.put("acl_id", value.getAclId());
										  result.put("published", value.getPublished());
										  result.put("embeds", value.getEmbeds());
										  result.put("subjects", tags.getOrDefault(value.getId(), List.of()));
										  return result;
									  })
		                              .toList(), page, size, total);
	}

	private PagedResponse<Map<String, Object>> galleryResponse(List<WebSiteGallery> values, int page, int size, long total) {
		Map<Long, List<fi.poltsi.vempain.website.controller.dto.response.SubjectResponse>> tags =
				subjects.forGalleries(values.stream()
				                            .map(WebSiteGallery::getId)
				                            .toList());
		return PagedResponse.of(values.stream()
		                              .map(value -> {
										  Map<String, Object> result = new LinkedHashMap<>();
										  result.put("id", value.getId());
										  result.put("gallery_id", value.getGalleryId());
										  result.put("shortname", value.getShortname());
										  result.put("description", value.getDescription());
										  result.put("acl_id", value.getAclId());
										  result.put("subjects", tags.getOrDefault(value.getId(), List.of()));
										  return result;
									  })
		                              .toList(), page, size, total);
	}

	private PagedResponse<Map<String, Object>> fileResponse(List<WebSiteFile> values, int page, int size, long total) {
		Map<Long, List<fi.poltsi.vempain.website.controller.dto.response.SubjectResponse>> tags =
				subjects.forFiles(values.stream()
				                        .map(WebSiteFile::getId)
				                        .toList());
		return PagedResponse.of(values.stream()
		                              .map(value -> {
										  Map<String, Object> result = new LinkedHashMap<>();
										  result.put("id", value.getId());
										  result.put("file_id", value.getFileId());
										  result.put("file_path", value.getFilePath());
										  result.put("thumbnail_path", value.getThumbnailPath());
										  result.put("mimetype", value.getMimetype());
										  result.put("acl_id", value.getAclId());
										  result.put("comment", value.getComment());
										  result.put("original_datetime", value.getOriginalDateTime());
										  result.put("width", value.getWidth());
										  result.put("height", value.getHeight());
										  result.put("length", value.getLength());
										  result.put("pages", value.getPages());
										  result.put("metadata", value.getMetadata());
										  result.put("subjects", tags.getOrDefault(value.getId(), List.of()));
										  return result;
									  })
		                              .toList(), page, size, total);
	}
}
