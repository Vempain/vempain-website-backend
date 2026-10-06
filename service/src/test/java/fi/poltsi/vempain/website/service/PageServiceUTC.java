package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.api.response.DirectoryNodeResponse;
import fi.poltsi.vempain.website.api.response.WebSitePageResponse;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
	private final WebSitePageRepository pages   = mock(WebSitePageRepository.class);
	private final ResponseMapperService mapper  = mock(ResponseMapperService.class);
	private final ResourceAccessService access  = mock(ResourceAccessService.class);
	private final PageService           service = new PageService(pages, mapper, access);

	@Test
	void pageDelegatesToTheMapper() {
		WebSitePage page = mock(WebSitePage.class);
		WebSitePageResponse response = WebSitePageResponse.builder()
														  .id(3L)
														  .build();
		when(mapper.page(page)).thenReturn(response);

		assertSame(response, service.page(page));
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
							   .get(0)
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
											.get(0)
											.getBody());
		assertEquals("a", service.directories()
								 .get(0)
								 .getName());
	}

	@Test
	void directoryTreeMarksLeavesAndNestsDirectories() {
		List<WebSitePage> found = List.of(pageWithPath("docs/readme"), pageWithPath("docs/guide/intro"), pageWithPath("docs/guide/setup"));
		when(pages.findByDirectoryForUser(1L, "docs/%")).thenReturn(found);

		List<DirectoryNodeResponse> tree = service.directoryTree("docs", 1);

		assertEquals(2, tree.size());
		DirectoryNodeResponse readme = tree.get(0);
		assertEquals("readme", readme.getTitle());
		assertEquals("docs/readme", readme.getKey());
		assertTrue(readme.getIsLeaf());
		assertNull(readme.getChildren());
		DirectoryNodeResponse guide = tree.get(1);
		assertEquals("guide", guide.getKey());
		assertFalse(guide.getIsLeaf());
		assertEquals(List.of("docs/guide/intro", "docs/guide/setup"), guide.getChildren()
																		   .stream()
																		   .map(DirectoryNodeResponse::getKey)
																		   .toList());
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
		WebSitePage page = mock(WebSitePage.class);
		when(page.getFilePath()).thenReturn(path);
		when(page.getAclId()).thenReturn(null);
		return page;
	}
}
