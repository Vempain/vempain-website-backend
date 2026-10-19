package fi.poltsi.vempain.website.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.SubjectSearchResponse;
import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubjectSearchService {

    private final WebSitePageRepository pageRepository;
    private final WebSiteGalleryRepository galleryRepository;
    private final WebSiteFileRepository fileRepository;
    private final ResponseMapperService mapper;
    private final WebsiteMetrics metrics;

    /**
     * Pages, galleries and files linked to any of the subjects, each independently paged. The search is timed and its total number of matches recorded.
     */
    public SubjectSearchResponse search(Collection<Long> subjectIds, int page, int size, long userId) {
        int p = Math.max(0, page);
        int n = Math.max(1, Math.min(50, size));
        if (subjectIds == null
            || subjectIds.isEmpty()) {
            return empty(p, n);
        }
        SubjectSearchResponse response = metrics.timeSearch(() -> find(subjectIds, p, n, userId));
        metrics.searchResults(response.getPages()
            .getTotalElements()
                + response.getGalleries()
                    .getTotalElements()
                + response.getFiles()
                    .getTotalElements());
        return response;
    }

    private SubjectSearchResponse find(Collection<Long> subjectIds, int p, int n, long userId) {
        return SubjectSearchResponse.builder()
            .pages(PagedResponse.of(mapper.pageSummaries(pageRepository.findBySubjectIdsForUser(subjectIds, userId, n, p * n)), p, n,
                    pageRepository.countBySubjectIdsForUser(subjectIds, userId)))
            .galleries(PagedResponse.of(mapper.galleries(galleryRepository.findBySubjectIdsForUser(subjectIds, userId, n, p * n)), p, n,
                    galleryRepository.countBySubjectIdsForUser(subjectIds, userId)))
            .files(PagedResponse.of(mapper.files(fileRepository.findBySubjectIdsForUser(subjectIds, userId, n, p * n)), p, n,
                    fileRepository.countBySubjectIdsForUser(subjectIds, userId)))
            .build();
    }

    private SubjectSearchResponse empty(int page, int size) {
        return SubjectSearchResponse.builder()
            .pages(PagedResponse.of(List.of(), page, size, 0))
            .galleries(PagedResponse.of(List.of(), page, size, 0))
            .files(PagedResponse.of(List.of(), page, size, 0))
            .build();
    }
}
