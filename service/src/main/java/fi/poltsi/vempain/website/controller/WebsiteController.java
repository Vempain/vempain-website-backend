package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.service.PageService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebsiteController implements WebsiteApi {
	private final PageService pages;

	public WebsiteController(PageService p) {
		pages = p;
	}

	public Object page(@PathVariable(required = false) String path) {
		String p = path == null || path.isBlank() ? "index" : path;
		if (p.equals("health") || p.startsWith("api/") || p.startsWith("file/")) {
			throw ApiException.notFound("Page not found");
		}
		WebSitePage page = pages.byPath(p);
		pages.require(page);
		return pages.page(page);
	}
}
