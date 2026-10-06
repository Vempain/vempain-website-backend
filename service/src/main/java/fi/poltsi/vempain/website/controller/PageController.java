package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.DirectoryNodeResponse;
import fi.poltsi.vempain.website.api.response.PageDirectoryResponse;
import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PageController implements PageApi {
	private final PageService         service;
	private final CurrentUserProvider user;

	public PagedResponse<WebSitePageResponse> pages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
													@RequestParam(defaultValue = "desc") String sort, @RequestParam(defaultValue = "") String search,
													@RequestParam(required = false) String path) {
		return service.list(page, size, sort, search, path, user.currentUserId());
	}

	public WebSitePageResponse page(@PathVariable long id) {
		WebSitePage page = service.byId(id);
		service.require(page);
		return service.page(page);
	}

	public PagedResponse<WebSitePageResponse> publicPages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
														  @RequestParam(defaultValue = "desc") String sort, @RequestParam(defaultValue = "") String search,
														  @RequestParam(required = false) String path) {
		return service.list(page, size, sort, search, path, user.currentUserId());
	}

	public List<WebSitePageResponse> children(@PathVariable long parentId) {
		return service.children(parentId, user.currentUserId());
	}

	public List<PageDirectoryResponse> directories() {
		return service.directories();
	}

	public List<DirectoryNodeResponse> tree(@PathVariable String directory) {
		return service.directoryTree(directory, user.currentUserId());
	}

	public WebSitePageResponse content(@RequestParam("file_path") String path) {
		WebSitePage page = service.byPath(path);
		service.require(page);
		return service.page(page);
	}
}
