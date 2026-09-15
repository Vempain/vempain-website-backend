package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.service.PageService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PageController implements PageApi {
	private final PageService         service;
	private final CurrentUserProvider user;

	public PageController(PageService service, CurrentUserProvider user) {
		this.service = service;
		this.user    = user;
	}

	public Object pages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
	             @RequestParam(defaultValue = "desc") String sort, @RequestParam(defaultValue = "") String search,
	             @RequestParam(required = false) String path) {
		return service.list(page, size, sort, search, path, user.currentUserId());
	}

	public Object page(@PathVariable long id) {
		WebSitePage p = service.byId(id);
		service.require(p);
		return service.page(p);
	}

	public Object publicPages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
	                   @RequestParam(defaultValue = "desc") String sort, @RequestParam(defaultValue = "") String search, @RequestParam(required = false) String path) {
		return service.list(page, size, sort, search, path, user.currentUserId());
	}

	public Object children(@PathVariable long parentId) {
		return service.children(parentId, user.currentUserId());
	}

	public Object directories() {
		return service.directories();
	}

	public Object tree(@PathVariable String directory) {
		return service.directoryTree(directory, user.currentUserId());
	}

	public Object content(@RequestParam("file_path") String path) {
		WebSitePage p = service.byPath(path);
		service.require(p);
		return service.page(p);
	}
}
