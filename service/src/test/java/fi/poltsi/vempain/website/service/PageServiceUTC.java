package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PageServiceUTC {
    private final WebSitePageRepository pages = mock(WebSitePageRepository.class);
    private final SubjectLookupService subjects = mock(SubjectLookupService.class);
    private final ResourceAccessService access = mock(ResourceAccessService.class);
    private final PageService service = new PageService(pages, subjects, access);

    @Test void pagePrefersRenderedCacheAndIncludesSubjects() {
        WebSitePage page = mock(WebSitePage.class);
        when(page.getId()).thenReturn(3L); when(page.getCache()).thenReturn("<p>safe</p>");
        when(page.getBody()).thenReturn("source"); when(page.getAclId()).thenReturn(2L);
        when(subjects.forPage(3L)).thenReturn(List.of());
        assertEquals("<p>safe</p>", service.page(page).get("body"));
        verify(subjects).forPage(3L);
    }

    @Test void listNormalizesPagingTokenizesSearchAndMapsTags() {
        WebSitePage page = mock(WebSitePage.class);
        when(page.getId()).thenReturn(3L);
        when(pages.findAccessiblePages(eq(0), eq(50), eq(List.of("hello world", "tag")), isNull(), eq("/x"), eq(5L)))
                .thenReturn(List.of(page));
        when(subjects.forPages(List.of(3L))).thenReturn(Map.of(3L, List.of()));
        when(pages.countAccessiblePages(List.of("hello world", "tag"), "/x", 5L)).thenReturn(1L);
        assertEquals(1, service.list(-1, 100, null, "\"hello world\" tag", "/x", 5L).content().size());
        verify(pages).findAccessiblePages(0, 50, List.of("hello world", "tag"), null, "/x", 5L);
    }

    @Test void lookupChildrenDirectoriesAndRequireDelegate() {
        when(pages.findByFilePath("/a")).thenReturn(Optional.empty());
        assertNull(service.byPath("/a"));
        when(pages.findByParentIdOrderByPublishedAsc(2L)).thenReturn(List.of());
        assertTrue(service.children(2L).isEmpty());
        when(pages.findTopLevelDirectories()).thenReturn(List.of("a"));
        assertEquals("a", service.directories().get(0).get("name"));
        WebSitePage treePage = pageWithPath("docs/readme");
        when(pages.findById(3L)).thenReturn(Optional.of(treePage));
        when(pages.findByDirectoryForUser(1L, "docs/%")).thenReturn(List.of(treePage));
        assertEquals("readme", service.directoryTree("docs", 1).get(0).get("title"));
        service.require(pageWithPath("ignored"));
        verify(access).requireAccess(null);
        assertThrows(ApiException.class, () -> service.directoryTree("", 1));
        assertThrows(ApiException.class, () -> service.require(null));
    }

    private WebSitePage pageWithPath(String path) {
        WebSitePage page = mock(WebSitePage.class);
        when(page.getFilePath()).thenReturn(path);
        when(page.getAclId()).thenReturn(null);
        return page;
    }
}
