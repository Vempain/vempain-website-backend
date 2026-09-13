package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.service.PageService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SubjectSearchController implements SubjectSearchApi {
	private final PageService pages;

	public SubjectSearchController(PageService pages) {
		this.pages = pages;
	}

	public Object search(@RequestBody(required = false) Map<String, Object> body) {
		return pages.list(intVal(body, "page", 0), intVal(body, "size", 12), "asc", String.valueOf(body == null ? "" : body.getOrDefault("search", "")), null, -1);
	}

	public Object searchIds(@RequestBody(required = false) Map<String, Object> body) {
		return pages.list(intVal(body, "page", 0), intVal(body, "size", 12), "asc", "", null, -1);
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
