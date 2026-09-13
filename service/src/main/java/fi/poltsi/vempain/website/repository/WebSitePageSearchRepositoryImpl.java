package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSitePage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WebSitePageSearchRepositoryImpl implements WebSitePageSearchRepository {

	private static final String FROM_CLAUSE =
			" FROM WebSitePage p LEFT JOIN WebSiteAcl a ON a.aclId = p.aclId WHERE "
			+ WebSitePageRepository.ACCESS_CONDITION;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<WebSitePage> findAccessiblePages(int page,
	                                             int perPage,
	                                             List<String> searchTerms,
	                                             String sortDirection,
	                                             String pathPrefix,
	                                             long userId) {
		Map<String, Object> parameters = new LinkedHashMap<>();
		String              where      = buildFilters(searchTerms, pathPrefix, parameters);
		String              direction  = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";

		TypedQuery<WebSitePage> query = entityManager.createQuery(
				"SELECT p" + FROM_CLAUSE + where + " ORDER BY p.published " + direction, WebSitePage.class);
		query.setParameter("userId", userId);
		parameters.forEach(query::setParameter);
		query.setFirstResult(page * perPage);
		query.setMaxResults(perPage);

		return query.getResultList();
	}

	@Override
	public long countAccessiblePages(List<String> searchTerms, String pathPrefix, long userId) {
		Map<String, Object> parameters = new LinkedHashMap<>();
		String              where      = buildFilters(searchTerms, pathPrefix, parameters);

		TypedQuery<Long> query = entityManager.createQuery(
				"SELECT COUNT(p.id)" + FROM_CLAUSE + where, Long.class);
		query.setParameter("userId", userId);
		parameters.forEach(query::setParameter);

		return query.getSingleResult();
	}

	private String buildFilters(List<String> searchTerms, String pathPrefix, Map<String, Object> parameters) {
		StringBuilder where = new StringBuilder();

		if (StringUtils.hasText(pathPrefix)) {
			where.append(" AND p.filePath LIKE :filePathPrefix");
			parameters.put("filePathPrefix", pathPrefix + "%");
		}

		List<String> terms = searchTerms == null ? List.of() : searchTerms;
		if (!terms.isEmpty()) {
			List<String> expressions = new ArrayList<>();
			for (int index = 0; index < terms.size(); index++) {
				String parameter = "term_" + index;
				expressions.add("(LOWER(p.title) LIKE :" + parameter
				                + " OR LOWER(p.header) LIKE :" + parameter
				                + " OR (p.cache IS NOT NULL AND LOWER(p.cache) LIKE :" + parameter + "))");
				parameters.put(parameter, "%" + terms.get(index)
				                                     .toLowerCase(Locale.ROOT) + "%");
			}
			where.append(" AND (")
			     .append(String.join(" OR ", expressions))
			     .append(")");
		}

		return where.toString();
	}
}
