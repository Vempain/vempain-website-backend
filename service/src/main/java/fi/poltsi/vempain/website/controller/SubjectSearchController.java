package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.controller.dto.response.SubjectSearchResponse;
import fi.poltsi.vempain.website.service.PageService;
import fi.poltsi.vempain.website.service.SubjectSearchService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SubjectSearchController implements SubjectSearchApi {
	private final PageService pages;
	private final SubjectSearchService subjectSearch;

	public SubjectSearchController(PageService pages, SubjectSearchService subjectSearch) {
		this.pages = pages;
		this.subjectSearch = subjectSearch;
	}

	public Object search(@RequestBody(required = false) Map<String, Object> body) {
		return pages.list(intVal(body, "page", 0), intVal(body, "size", 12), "asc", String.valueOf(body == null ? "" : body.getOrDefault("search", "")), null, -1);
	}

	public SubjectSearchResponse searchIds(@RequestBody(required = false) Map<String, Object> body) {
		return subjectSearch.search(longList(body, "subject_ids"), intVal(body, "page", 0), intVal(body, "size", 12), -1);
	}

	private List<Long> longList(Map<String, Object> body, String key) {
		if (body == null || !(body.get(key) instanceof List<?> values)) {
			return List.of();
		}
		return values.stream()
		             .map(value -> {
						 try {
							 return Long.parseLong(value.toString());
						 } catch (NumberFormatException ignored) {
							 return null;
						 }
					 })
		             .filter(value -> value != null && value > 0)
		             .toList();
	}

	private int intVal(Map<String, Object> b, String k, int d) {
		if (b == null || b.get(k) == null) {
			return d;
		}
		try {
			return Integer.parseInt(b.get(k)
			                         .toString());
		} catch (Exception e) {
			return d;
		}
	}
}
