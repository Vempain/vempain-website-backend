package fi.poltsi.vempain.website.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.poltsi.vempain.website.api.response.DirectoryNodeResponse;
import fi.poltsi.vempain.website.api.response.PageDirectoryResponse;
import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import fi.poltsi.vempain.website.service.PageService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PageController implements PageApi {

    private final PageService service;
    private final CurrentUserProvider user;
    private final WebsiteMetrics metrics;

    public PagedResponse<WebSitePageResponse> pages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "desc") String sort, @RequestParam(defaultValue = "") String search, @RequestParam(required = false) String path) {
        return service.list(page, size, sort, search, path, user.currentUserId());
    }

    public WebSitePageResponse page(@PathVariable long id) {
        return view(service.byId(id));
    }

    public PagedResponse<WebSitePageResponse> publicPages(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "desc") String sort, @RequestParam(defaultValue = "") String search, @RequestParam(required = false) String path) {
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
        return view(service.byPath(path));
    }

    /**
     * A page the caller may read counts as a view of its path; a page that does not exist counts under {@code unknown} so that the tag set stays bounded.
     */
    private WebSitePageResponse view(WebSitePage page) {
        if (page == null) {
            metrics.pageView(WebsiteMetrics.UNKNOWN_PATH);
        }
        service.require(page);
        metrics.pageView(page.getFilePath());
        return service.page(page);
    }
}
