package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSitePage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The search repository builds its JPQL from constants and binds every request value as a parameter; the LIKE patterns are
 * escaped so that request text cannot act as wildcards.
 */
class WebSitePageSearchRepositoryImplUTC {

	private final EntityManager                   entityManager = mock(EntityManager.class);
	@SuppressWarnings("unchecked")
	private final TypedQuery<WebSitePage>         query         = mock(TypedQuery.class);
	private final WebSitePageSearchRepositoryImpl repository    = new WebSitePageSearchRepositoryImpl();

	WebSitePageSearchRepositoryImplUTC() {
		ReflectionTestUtils.setField(repository, "entityManager", entityManager);
		when(entityManager.createQuery(anyString(), eq(WebSitePage.class))).thenReturn(query);
		when(query.getResultList()).thenReturn(List.of());
	}

	@Test
	void searchTermsAndPathPrefixAreBoundAsEscapedPatterns() {
		repository.findAccessiblePages(0, 12, List.of("50%", "a_b", "Sunset"), "' OR 1=1 --", "trips/2024_x", 7L);

		var jpql = ArgumentCaptor.forClass(String.class);
		verify(entityManager).createQuery(jpql.capture(), eq(WebSitePage.class));
		assertTrue(jpql.getValue()
					   .endsWith(" ORDER BY p.published DESC"), "an unknown sort direction falls back to the DESC constant");
		assertFalse(jpql.getValue()
						.contains("50%"), "request text never appears in the JPQL");
		verify(query).setParameter("userId", 7L);
		verify(query).setParameter("filePathPrefix", "trips/2024\\_x%");
		verify(query).setParameter("term_0", "%50\\%%");
		verify(query).setParameter("term_1", "%a\\_b%");
		verify(query).setParameter("term_2", "%sunset%");
		verify(query).setFirstResult(0);
		verify(query).setMaxResults(12);
	}

	@Test
	void onlyTheFirstTermsAreUsed() {
		var max = WebSitePageSearchRepositoryImpl.MAX_SEARCH_TERMS;
		var terms = IntStream.range(0, max + 5)
							 .mapToObj(i -> "t" + i)
							 .toList();

		repository.findAccessiblePages(1, 10, terms, "asc", null, 1L);

		verify(query).setParameter("term_" + (max - 1), "%t" + (max - 1) + "%");
		verify(query, never()).setParameter(eq("term_" + max), anyString());
		verify(query, never()).setParameter(eq("filePathPrefix"), anyString());
		verify(query).setFirstResult(10);
	}

	@Test
	void countUsesTheSameFilters() {
		@SuppressWarnings("unchecked")
		TypedQuery<Long> countQuery = mock(TypedQuery.class);
		when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
		when(countQuery.getSingleResult()).thenReturn(3L);

		assertEquals(3L, repository.countAccessiblePages(List.of("x%"), "docs", 2L));
		verify(countQuery).setParameter("term_0", "%x\\%%");
		verify(countQuery).setParameter("filePathPrefix", "docs%");
	}
}
