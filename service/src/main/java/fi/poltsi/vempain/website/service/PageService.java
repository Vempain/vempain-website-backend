package fi.poltsi.vempain.website.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import fi.poltsi.vempain.website.api.response.DirectoryNodeResponse;
import fi.poltsi.vempain.website.api.response.PageDirectoryResponse;
import fi.poltsi.vempain.website.api.response.PagedResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PageService {

    private static final Pattern SEARCH = Pattern.compile("\"([^\"]+)\"|(\\S+)");
    /** The page that stands for its directory in the navigation */
    private static final String INDEX_PAGE = "index";

    private final WebSitePageRepository pages;
    private final ResponseMapperService mapper;
    private final ResourceAccessService access;
    private final OpenGraphService openGraph;

    /**
     * Full page response with the rendered body and the Open Graph metadata of the page.
     */
    public WebSitePageResponse page(WebSitePage page) {
        WebSitePageResponse response = mapper.page(page);
        response.setOpenGraph(openGraph.forPage(page));
        return response;
    }

    /**
     * Paged page summaries filtered by search terms and path prefix.
     */
    public PagedResponse<WebSitePageResponse> list(int page, int size, String sort, String search, String path, long userId) {
        int p = Math.max(0, page);
        int n = Math.max(1, Math.min(50, size));
        List<String> terms = tokenize(search);
        List<WebSitePage> found = pages.findAccessiblePages(p, n, terms, sort, path, userId);
        return PagedResponse.of(mapper.pageSummaries(found), p, n, pages.countAccessiblePages(terms, path, userId));
    }

    public WebSitePage byPath(String path) {
        return pages.findByFilePath(path)
            .orElse(null);
    }

    public WebSitePage byId(long id) {
        return pages.findById(id)
            .orElse(null);
    }

    /**
     * Child pages with their bodies.
     */
    public List<WebSitePageResponse> children(long parent, long userId) {
        return mapper.pages(pages.findByParentIdForUser(parent, userId));
    }

    public List<PageDirectoryResponse> directories() {
        return pages.findTopLevelDirectories()
            .stream()
            .map(name -> PageDirectoryResponse.builder()
                .name(name)
                .build())
            .toList();
    }

    /**
     * Builds the directory tree below {@code directory}. Intermediate nodes carry the path segment as key, leaves carry the full page path and
     * {@code is_leaf = true}. Every node carries its path segment as {@code name} and, as {@code title}, the label the navigation shows: the header of the
     * page, for a directory the header of its {@code index} page, and the segment when there is no header.
     */
    public List<DirectoryNodeResponse> directoryTree(String directory, long userId) {
        if (directory == null
            || directory.isBlank()) {
            throw ApiException.badRequest("Directory is required");
        }
        List<DirectoryNodeResponse> root = new ArrayList<>();
        for (WebSitePage page : pages.findByDirectoryForUser(userId, directory + "/%")) {
            String relative = page.getFilePath()
                .substring(Math.min(page.getFilePath()
                    .length(), directory.length() + 1));
            String[] parts = relative.split("/");
            List<DirectoryNodeResponse> level = root;
            DirectoryNodeResponse parent = null;
            for (int i = 0; i < parts.length; i++) {
                String key = parts[i];
                boolean leaf = i == parts.length - 1;
                DirectoryNodeResponse node = level.stream()
                    .filter(candidate -> key.equals(candidate.getName()))
                    .findFirst()
                    .orElse(null);
                if (node == null) {
                    node = DirectoryNodeResponse.builder()
                        .title(leaf ? labelOf(page, key) : key)
                        .name(key)
                        .key(leaf ? page.getFilePath() : key)
                        .isLeaf(leaf)
                        .children(leaf ? null : new ArrayList<>())
                        .build();
                    level.add(node);
                }
                if (leaf
                    && INDEX_PAGE.equals(key)
                    && parent != null) {
                    // The directory node stands for its index page in the navigation, so it shows that page's header
                    parent.setTitle(labelOf(page, parent.getName()));
                }
                parent = node;
                if (!leaf) {
                    if (node.getChildren() == null) {
                        node.setChildren(new ArrayList<>());
                        node.setIsLeaf(false);
                    }
                    level = node.getChildren();
                }
            }
        }
        return root;
    }

    public void require(WebSitePage page) {
        if (page == null) {
            throw ApiException.notFound("Page not found");
        }
        access.requireAccess(page.getAclId());
    }

    /** The navigation label of a page: its header, or the path segment when the header is blank */
    private static String labelOf(WebSitePage page, String segment) {
        String header = page.getHeader();
        return header == null
               || header.isBlank() ? segment : header.strip();
    }

    private List<String> tokenize(String search) {
        if (search == null) {
            return List.of();
        }
        Matcher matcher = SEARCH.matcher(search.trim());
        List<String> out = new ArrayList<>();
        while (matcher.find()) {
            out.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
        }
        return out;
    }
}
