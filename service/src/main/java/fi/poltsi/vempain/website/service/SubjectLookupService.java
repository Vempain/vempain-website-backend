package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.entity.WebSiteSubject;
import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository;
import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository.SubjectLink;
import fi.poltsi.vempain.website.controller.dto.response.SubjectResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Loads the subjects attached to pages, files and galleries. Subjects are resolved in bulk
 * to avoid a query per listed row, and are ordered case insensitively by the default
 * subject text just like the PHP repository did.
 */
@Service
public class SubjectLookupService {

	private static final Comparator<WebSiteSubject> BY_SUBJECT =
			Comparator.comparing(subject -> subject.getSubject() == null ? "" : subject.getSubject(),
			                     String.CASE_INSENSITIVE_ORDER);

	private final WebSiteSubjectRepository subjectRepository;

	public SubjectLookupService(WebSiteSubjectRepository subjectRepository) {
		this.subjectRepository = subjectRepository;
	}

	public List<SubjectResponse> forPage(Long pageId) {
		return forSingle(pageId, subjectRepository::findPageSubjectLinks);
	}

	public List<SubjectResponse> forFile(Long fileId) {
		return forSingle(fileId, subjectRepository::findFileSubjectLinks);
	}

	public List<SubjectResponse> forGallery(Long galleryId) {
		return forSingle(galleryId, subjectRepository::findGallerySubjectLinks);
	}

	public Map<Long, List<SubjectResponse>> forPages(Collection<Long> pageIds) {
		return group(pageIds, subjectRepository::findPageSubjectLinks);
	}

	public Map<Long, List<SubjectResponse>> forFiles(Collection<Long> fileIds) {
		return group(fileIds, subjectRepository::findFileSubjectLinks);
	}

	public Map<Long, List<SubjectResponse>> forGalleries(Collection<Long> galleryIds) {
		return group(galleryIds, subjectRepository::findGallerySubjectLinks);
	}

	private List<SubjectResponse> forSingle(Long resourceId,
	                                        Function<Collection<Long>, List<SubjectLink>> linkLoader) {
		if (resourceId == null) {
			return List.of();
		}

		return group(List.of(resourceId), linkLoader).getOrDefault(resourceId, List.of());
	}

	private Map<Long, List<SubjectResponse>> group(Collection<Long> resourceIds,
	                                               Function<Collection<Long>, List<SubjectLink>> linkLoader) {
		Set<Long> ids = new LinkedHashSet<>();
		if (resourceIds != null) {
			resourceIds.stream()
			           .filter(id -> id != null && id != 0L)
			           .forEach(ids::add);
		}
		if (ids.isEmpty()) {
			return Map.of();
		}

		List<SubjectLink> links = linkLoader.apply(ids);
		if (links.isEmpty()) {
			return Map.of();
		}

		Set<Long> subjectIds = new LinkedHashSet<>();
		links.forEach(link -> subjectIds.add(link.getSubjectId()));

		Map<Long, WebSiteSubject> subjectsById = new HashMap<>();
		subjectRepository.findByIdIn(subjectIds)
		                 .forEach(subject -> subjectsById.put(subject.getId(), subject));

		Map<Long, List<WebSiteSubject>> grouped = new HashMap<>();
		for (SubjectLink link : links) {
			WebSiteSubject subject = subjectsById.get(link.getSubjectId());
			if (subject != null) {
				grouped.computeIfAbsent(link.getResourceId(), key -> new ArrayList<>())
				       .add(subject);
			}
		}

		Map<Long, List<SubjectResponse>> result = new HashMap<>();
		grouped.forEach((resourceId, subjects) -> {
			subjects.sort(BY_SUBJECT);
			result.put(resourceId, subjects.stream()
			                               .map(subject -> new SubjectResponse(
					                               subject.getId(),
					                               subject.getSubject(),
					                               subject.getSubjectDe(),
					                               subject.getSubjectEn(),
					                               subject.getSubjectEs(),
					                               subject.getSubjectFi(),
					                               subject.getSubjectSe()))
			                               .toList());
		});

		return result;
	}
}
