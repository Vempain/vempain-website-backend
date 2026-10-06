package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.request.SubjectIdSearchRequest;
import fi.poltsi.vempain.website.api.request.SubjectSearchRequest;
import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.SubjectSearchResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.auth.AuthenticatedUser;
import fi.poltsi.vempain.website.service.PageService;
import fi.poltsi.vempain.website.service.SubjectSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public search controls. Both endpoints search as the anonymous user, so only public content is returned.
 */
@RestController
@RequiredArgsConstructor
public class SubjectSearchController implements SubjectSearchApi {
	private static final int DEFAULT_PAGE_SIZE = 12;

	private final PageService pages;
	private final SubjectSearchService subjectSearch;

	public PagedResponse<WebSitePageResponse> search(@RequestBody(required = false) SubjectSearchRequest request) {
		SubjectSearchRequest criteria = request == null ? new SubjectSearchRequest() : request;
		return pages.list(valueOrDefault(criteria.getPage(), 0), valueOrDefault(criteria.getSize(), DEFAULT_PAGE_SIZE), "asc",
						  criteria.getSearch() == null ? "" : criteria.getSearch(), null, AuthenticatedUser.ANONYMOUS_USER_ID);
	}

	public SubjectSearchResponse searchIds(@RequestBody(required = false) SubjectIdSearchRequest request) {
		SubjectIdSearchRequest criteria = request == null ? new SubjectIdSearchRequest() : request;
		List<Long> subjectIds = criteria.getSubjectIds() == null ? List.of() : criteria.getSubjectIds()
																					   .stream()
																					   .filter(id -> id != null && id > 0)
																					   .toList();
		return subjectSearch.search(subjectIds, valueOrDefault(criteria.getPage(), 0), valueOrDefault(criteria.getSize(), DEFAULT_PAGE_SIZE),
									AuthenticatedUser.ANONYMOUS_USER_ID);
	}

	private static int valueOrDefault(Integer value, int defaultValue) {
		return value == null ? defaultValue : value;
	}
}
