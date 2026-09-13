package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSitePage;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WebSitePageRepository extends JpaRepository<WebSitePage, Long>, WebSitePageSearchRepository {

	/**
	 * A page is visible when the left joined ACL produced no row, when the user is a member
	 * of its ACL, or when the user has the global permission flag.
	 */
	String ACCESS_CONDITION = """
			(a.aclId IS NULL
			 OR a.userId = :userId
			 OR EXISTS (SELECT 1 FROM WebSiteUser wsu WHERE wsu.id = :userId AND wsu.globalPermission = TRUE))
			""";

	Optional<WebSitePage> findByFilePath(String filePath);

	List<WebSitePage> findByIdIn(Collection<Long> ids);

	List<WebSitePage> findByParentIdOrderByPublishedAsc(Long parentId);

	@Query("SELECT p FROM WebSitePage p LEFT JOIN WebSiteAcl a ON a.aclId = p.aclId WHERE " + ACCESS_CONDITION
	       + " AND p.filePath LIKE :directoryPrefix"
	       + " ORDER BY p.filePath ASC")
	List<WebSitePage> findByDirectoryForUser(@Param("userId") long userId,
	                                         @Param("directoryPrefix") String directoryPrefix);

	@Query("SELECT p FROM WebSitePage p LEFT JOIN WebSiteAcl a ON a.aclId = p.aclId WHERE " + ACCESS_CONDITION
	       + " ORDER BY p.published DESC, p.id DESC")
	List<WebSitePage> findLatestAccessible(@Param("userId") long userId, Limit limit);

	@Query(value = """
			SELECT DISTINCT split_part(file_path, '/', 1) AS top_level_directory
			FROM web_site_page
			WHERE file_path LIKE '%/%'
			ORDER BY top_level_directory ASC
			""", nativeQuery = true)
	List<String> findTopLevelDirectories();

	@Query(value = """
			SELECT id,
			       title,
			       header,
			       file_path AS filePath,
			       CASE WHEN published IS NULL THEN NULL
			            ELSE to_char(published, 'YYYY-MM-DD"T"HH24:MI:SS') END AS published
			FROM web_site_page
			WHERE published IS NOT NULL
			  AND to_char(published::date, 'MM-DD') = to_char(CURRENT_DATE, 'MM-DD')
			ORDER BY random()
			LIMIT :maxResults
			""", nativeQuery = true)
	List<SameDatePage> findRandomPublishedByCurrentMonthDay(@Param("maxResults") int maxResults);

	/**
	 * Same-date random page used by the {@code today_random} embed.
	 */
	interface SameDatePage {
		Long getId();

		String getTitle();

		String getHeader();

		String getFilePath();

		String getPublished();
	}
}
