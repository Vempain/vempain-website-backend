package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSiteSubject;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WebSiteSubjectRepository extends JpaRepository<WebSiteSubject, Long> {

	List<WebSiteSubject> findByIdIn(Collection<Long> ids);

	@Query(value = "SELECT page_id AS resourceId, subject_id AS subjectId FROM web_site_page_subject WHERE page_id IN (:resourceIds)",
	       nativeQuery = true)
	List<SubjectLink> findPageSubjectLinks(@Param("resourceIds") Collection<Long> resourceIds);

	@Query(value = "SELECT file_id AS resourceId, subject_id AS subjectId FROM web_site_file_subject WHERE file_id IN (:resourceIds)",
	       nativeQuery = true)
	List<SubjectLink> findFileSubjectLinks(@Param("resourceIds") Collection<Long> resourceIds);

	@Query(value = "SELECT gallery_id AS resourceId, subject_id AS subjectId FROM web_site_gallery_subject WHERE gallery_id IN (:resourceIds)",
	       nativeQuery = true)
	List<SubjectLink> findGallerySubjectLinks(@Param("resourceIds") Collection<Long> resourceIds);

	@Query("SELECT s FROM WebSiteSubject s WHERE LOWER(s.subject) LIKE :term ORDER BY s.subject ASC")
	List<WebSiteSubject> autocomplete(@Param("term") String term, Limit limit);

	/**
	 * Top tags used by the word cloud embed. Counts every reference from pages, files and
	 * galleries, grouped by the lower cased and trimmed subject text.
	 */
	@Query(value = """
			SELECT tag_counts.tag_text AS text, tag_counts.usage_count AS value
			FROM (
			    SELECT LOWER(TRIM(s.subject)) AS tag_text, COUNT(*) AS usage_count
			    FROM (
			        SELECT subject_id FROM web_site_page_subject
			        UNION ALL
			        SELECT subject_id FROM web_site_file_subject
			        UNION ALL
			        SELECT subject_id FROM web_site_gallery_subject
			    ) refs
			    INNER JOIN web_site_subject s ON s.id = refs.subject_id
			    WHERE s.subject IS NOT NULL
			      AND TRIM(s.subject) <> ''
			    GROUP BY LOWER(TRIM(s.subject))
			) tag_counts
			ORDER BY tag_counts.usage_count DESC, tag_counts.tag_text ASC
			LIMIT :maxResults
			""", nativeQuery = true)
	List<TagCount> findMostUsedTags(@Param("maxResults") int maxResults);

	/**
	 * Resource identifier to subject identifier pair read from one of the pivot tables.
	 */
	interface SubjectLink {
		Long getResourceId();

		Long getSubjectId();
	}

	/**
	 * Tag together with the number of times it is used across pages, files and galleries.
	 */
	interface TagCount {
		String getText();

		Long getValue();
	}
}
