package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSitePage;

import java.util.List;

/**
 * Page listing with an arbitrary number of search terms, which cannot be expressed as a
 * static query. Every term has to match the title, the header or the rendered cache.
 */
public interface WebSitePageSearchRepository {

	List<WebSitePage> findAccessiblePages(int page,
	                                      int perPage,
	                                      List<String> searchTerms,
	                                      String sortDirection,
	                                      String pathPrefix,
	                                      long userId);

	long countAccessiblePages(List<String> searchTerms, String pathPrefix, long userId);
}
