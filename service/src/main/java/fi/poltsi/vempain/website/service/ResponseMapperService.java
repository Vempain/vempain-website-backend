package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.api.response.LocationResponse;
import fi.poltsi.vempain.website.api.response.PageEmbedResponse;
import fi.poltsi.vempain.website.api.response.SubjectResponse;
import fi.poltsi.vempain.website.api.response.WebSiteFileResponse;
import fi.poltsi.vempain.website.api.response.WebSiteGalleryResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebGpsLocation;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.repository.WebGpsLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps site entities to the response DTOs of the {@code api} module. Subjects and GPS locations are
 * resolved in bulk so that a listed row never costs a query of its own. GPS locations are only
 * resolved for authenticated callers; anonymous callers receive {@code location: null}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseMapperService {

	private static final TypeReference<List<PageEmbedResponse>> EMBED_LIST = new TypeReference<>() {
	};

	private final SubjectLookupService     subjects;
	private final WebGpsLocationRepository locations;
	private final CurrentUserProvider      currentUser;
	private final ObjectMapper             objectMapper;

	/**
	 * Full page response including the rendered body and the audit fields.
	 */
	public WebSitePageResponse page(WebSitePage page) {
		return pages(List.of(page)).get(0);
	}

	/**
	 * Full page responses including the rendered body and the audit fields.
	 */
	public List<WebSitePageResponse> pages(List<WebSitePage> pages) {
		Map<Long, List<SubjectResponse>> tags = subjects.forPages(ids(pages, WebSitePage::getId));
		return pages.stream()
					.map(page -> summary(page, tags).body(renderedBody(page))
													.pageStyle(page.getPageStyle())
													.creator(page.getCreator())
													.created(page.getCreated())
													.modifier(page.getModifier())
													.modified(page.getModified())
													.build())
					.toList();
	}

	/**
	 * Page summaries as used by list endpoints: identifiers, titles, path, publish time, embeds and subjects.
	 */
	public List<WebSitePageResponse> pageSummaries(List<WebSitePage> pages) {
		Map<Long, List<SubjectResponse>> tags = subjects.forPages(ids(pages, WebSitePage::getId));
		return pages.stream()
					.map(page -> summary(page, tags).build())
					.toList();
	}

	public WebSiteFileResponse file(WebSiteFile file) {
		return files(List.of(file)).get(0);
	}

	public List<WebSiteFileResponse> files(List<WebSiteFile> files) {
		Map<Long, List<SubjectResponse>> tags             = subjects.forFiles(ids(files, WebSiteFile::getId));
		Map<Long, LocationResponse>      resolvedLocation = locations(files);
		return files.stream()
					.map(file -> WebSiteFileResponse.builder()
													.id(file.getId())
													.fileId(file.getFileId())
													.aclId(file.getAclId())
													.comment(file.getComment())
													.filePath(file.getFilePath())
													.thumbnailPath(file.getThumbnailPath())
													.mimetype(file.getMimetype())
													.originalDateTime(file.getOriginalDateTime())
													.rightsHolder(file.getRightsHolder())
													.rightsTerms(file.getRightsTerms())
													.rightsUrl(file.getRightsUrl())
													.creatorName(file.getCreatorName())
													.creatorEmail(file.getCreatorEmail())
													.creatorCountry(file.getCreatorCountry())
													.creatorUrl(file.getCreatorUrl())
													.locationId(file.getLocationId())
													.location(file.getLocationId() == null ? null : resolvedLocation.get(file.getLocationId()))
													.width(file.getWidth())
													.height(file.getHeight())
													.length(file.getLength())
													.pages(file.getPages())
													.metadata(file.getMetadata())
													.subjects(tags.getOrDefault(file.getId(), List.of()))
													.build())
					.toList();
	}

	public WebSiteGalleryResponse gallery(WebSiteGallery gallery) {
		return galleries(List.of(gallery)).get(0);
	}

	public List<WebSiteGalleryResponse> galleries(List<WebSiteGallery> galleries) {
		Map<Long, List<SubjectResponse>> tags = subjects.forGalleries(ids(galleries, WebSiteGallery::getId));
		return galleries.stream()
						.map(gallery -> WebSiteGalleryResponse.builder()
															  .id(gallery.getId())
															  .galleryId(gallery.getGalleryId())
															  .shortname(gallery.getShortname())
															  .description(gallery.getDescription())
															  .aclId(gallery.getAclId())
															  .subjects(tags.getOrDefault(gallery.getId(), List.of()))
															  .build())
						.toList();
	}

	public LocationResponse location(WebGpsLocation location) {
		return LocationResponse.builder()
							   .id(location.getId())
							   .latitude(location.getLatitude())
							   .latitudeRef(location.getLatitudeRef())
							   .longitude(location.getLongitude())
							   .longitudeRef(location.getLongitudeRef())
							   .altitude(location.getAltitude())
							   .direction(location.getDirection())
							   .satelliteCount(location.getSatelliteCount())
							   .country(location.getCountry())
							   .state(location.getState())
							   .city(location.getCity())
							   .street(location.getStreet())
							   .subLocation(location.getSubLocation())
							   .build();
	}

	/**
	 * Parses the embed definitions stored as JSON text with the page. Missing, blank or malformed
	 * text yields {@code null}; a malformed value is logged because it points to a publishing defect.
	 */
	public List<PageEmbedResponse> embeds(String embedJson) {
		if (embedJson == null || embedJson.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(embedJson, EMBED_LIST);
		} catch (JacksonException e) {
			log.warn("Ignoring malformed page embeds: {}", e.getOriginalMessage());
			return null;
		}
	}

	/**
	 * The publisher-rendered cache is preferred over the source body. The body is never interpreted.
	 */
	public static String renderedBody(WebSitePage page) {
		return page.getCache() != null && !page.getCache()
											   .isEmpty() ? page.getCache() : page.getBody();
	}

	private WebSitePageResponse.WebSitePageResponseBuilder summary(WebSitePage page, Map<Long, List<SubjectResponse>> tags) {
		return WebSitePageResponse.builder()
								  .id(page.getId())
								  .pageId(page.getPageId())
								  .title(page.getTitle())
								  .header(page.getHeader())
								  .filePath(page.getFilePath())
								  .secure(page.isSecure())
								  .aclId(page.getAclId())
								  .published(page.getPublished())
								  .embeds(embeds(page.getEmbeds()))
								  .subjects(tags.getOrDefault(page.getId(), List.of()));
	}

	private Map<Long, LocationResponse> locations(List<WebSiteFile> files) {
		if (currentUser.current()
					   .isEmpty()) {
			return Map.of();
		}
		Set<Long> locationIds = files.stream()
									 .map(WebSiteFile::getLocationId)
									 .filter(Objects::nonNull)
									 .collect(Collectors.toSet());
		if (locationIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, LocationResponse> result = new HashMap<>();
		locations.findByIdIn(locationIds)
				 .forEach(location -> result.put(location.getId(), location(location)));
		return result;
	}

	private static <T> List<Long> ids(List<T> values, Function<T, Long> idGetter) {
		return values.stream()
					 .map(idGetter)
					 .toList();
	}
}
