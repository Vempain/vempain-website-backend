package fi.poltsi.vempain.website.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.website.api.response.DirectoryNodeResponse;
import fi.poltsi.vempain.website.api.response.OpenGraphResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageServiceUTC {

    private final WebSitePageRepository pages = mock(WebSitePageRepository.class);
    private final ResponseMapperService mapper = mock(ResponseMapperService.class);
    private final ResourceAccessService access = mock(ResourceAccessService.class);
    private final OpenGraphService openGraph = mock(OpenGraphService.class);
    private final PageService service = new PageService(pages, mapper, access, openGraph);

    @Test
    void pageDelegatesToTheMapperAndAttachesTheOpenGraphMetadata() {
        WebSitePage page = mock(WebSitePage.class);
        WebSitePageResponse response = WebSitePageResponse.builder()
            .id(3L)
            .build();
        OpenGraphResponse metadata = OpenGraphResponse.builder()
            .title("Page")
            .build();
        when(mapper.page(page)).thenReturn(response);
        when(openGraph.forPage(page)).thenReturn(metadata);

        assertSame(response, service.page(page));
        assertSame(metadata, response.getOpenGraph());
    }

    @Test
    void listNormalizesPagingTokenizesSearchAndMapsSummaries() {
        WebSitePage page = mock(WebSitePage.class);
        when(pages.findAccessiblePages(eq(0), eq(50), eq(List.of("hello world", "tag")), isNull(), eq("/x"), eq(5L))).thenReturn(List.of(page));
        when(mapper.pageSummaries(List.of(page))).thenReturn(List.of(WebSitePageResponse.builder()
            .id(3L)
            .build()));
        when(pages.countAccessiblePages(List.of("hello world", "tag"), "/x", 5L)).thenReturn(1L);

        var result = service.list(-1, 100, null, "\"hello world\" tag", "/x", 5L);

        assertEquals(1, result.getContent()
            .size());
        assertEquals(3L, result.getContent()
            .getFirst()
            .getId());
        assertEquals(50, result.getSize());
        assertTrue(result.isLast());
        verify(pages).findAccessiblePages(0, 50, List.of("hello world", "tag"), null, "/x", 5L);
    }

    @Test
    void childrenAndDirectoriesAreTyped() {
        WebSitePage child = mock(WebSitePage.class);
        when(pages.findByParentIdForUser(2L, -1L)).thenReturn(List.of(child));
        when(mapper.pages(List.of(child))).thenReturn(List.of(WebSitePageResponse.builder()
            .id(9L)
            .body("<p>Child</p>")
            .build()));
        when(pages.findTopLevelDirectories()).thenReturn(List.of("a"));

        assertEquals("<p>Child</p>", service.children(2L, -1L)
            .getFirst()
            .getBody());
        assertEquals("a", service.directories()
            .getFirst()
            .getName());
    }

    @Test
    void directoryTreeMarksLeavesAndNestsDirectories() {
        List<WebSitePage> found = List.of(pageWithPath("docs/readme", "Read me first"), pageWithPath("docs/guide/intro", "Introduction"),
                pageWithPath("docs/guide/setup", "  Setting up  "));
        when(pages.findByDirectoryForUser(1L, "docs/%")).thenReturn(found);

        List<DirectoryNodeResponse> tree = service.directoryTree("docs", 1);

        assertEquals(2, tree.size());
        DirectoryNodeResponse readme = tree.getFirst();
        assertEquals("Read me first", readme.getTitle());
        assertEquals("readme", readme.getName());
        assertEquals("docs/readme", readme.getKey());
        assertTrue(readme.getIsLeaf());
        assertNull(readme.getChildren());
        DirectoryNodeResponse guide = tree.get(1);
        assertEquals("guide", guide.getKey());
        assertEquals("guide", guide.getName());
        assertEquals("guide", guide.getTitle(), "a directory without an index page is labelled by its segment");
        assertFalse(guide.getIsLeaf());
        assertEquals(List.of("docs/guide/intro", "docs/guide/setup"), guide.getChildren()
            .stream()
            .map(DirectoryNodeResponse::getKey)
            .toList());
        assertEquals(List.of("Introduction", "Setting up"), guide.getChildren()
            .stream()
            .map(DirectoryNodeResponse::getTitle)
            .toList());
    }

    @Test
    void directoryTreeLabelsNodesWithPageHeaders() {
        // The index page is listed after a sibling on purpose: the directory label must follow the index page whenever it turns up
        List<WebSitePage> found = List.of(pageWithPath("docs/trips/2024", "Summer 2024"), pageWithPath("docs/trips/index", "All trips"),
                pageWithPath("docs/blank", "   "), pageWithPath("docs/none", null), pageWithPath("docs/index", "Documentation"));
        when(pages.findByDirectoryForUser(1L, "docs/%")).thenReturn(found);

        List<DirectoryNodeResponse> tree = service.directoryTree("docs", 1);

        assertEquals(List.of("trips", "blank", "none", "index"), tree.stream()
            .map(DirectoryNodeResponse::getName)
            .toList());
        DirectoryNodeResponse trips = tree.getFirst();
        assertEquals("All trips", trips.getTitle(), "a directory shows the header of its index page");
        assertEquals(List.of("Summer 2024", "All trips"), trips.getChildren()
            .stream()
            .map(DirectoryNodeResponse::getTitle)
            .toList());
        assertEquals("blank", tree.get(1)
            .getTitle(), "a blank header falls back to the segment");
        assertEquals("none", tree.get(2)
            .getTitle(), "a missing header falls back to the segment");
        assertEquals("Documentation", tree.get(3)
            .getTitle(), "the index page of the requested directory itself is a plain leaf");
        assertEquals("docs/index", tree.get(3)
            .getKey());
    }

    @Test
    void lookupsAndRequireDelegate() {
        when(pages.findByFilePath("/a")).thenReturn(Optional.empty());
        assertNull(service.byPath("/a"));
        when(pages.findById(3L)).thenReturn(Optional.empty());
        assertNull(service.byId(3L));
        service.require(pageWithPath("ignored"));
        verify(access).requireAccess(null);
        assertThrows(ApiException.class, () -> service.directoryTree("", 1));
        assertThrows(ApiException.class, () -> service.require(null));
    }

    private WebSitePage pageWithPath(String path) {
        return pageWithPath(path, null);
    }

    private WebSitePage pageWithPath(String path, String header) {
        WebSitePage page = mock(WebSitePage.class);
        when(page.getFilePath()).thenReturn(path);
        when(page.getHeader()).thenReturn(header);
        when(page.getAclId()).thenReturn(null);
        return page;
    }
}
