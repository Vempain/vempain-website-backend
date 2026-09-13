package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSiteFile;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WebSiteFileRepository extends JpaRepository<WebSiteFile, Long> {

	/**
	 * A file is visible when the left joined ACL produced no row, when the user is a member
	 * of its ACL, or when the user has the global permission flag.
	 */
	String ACCESS_CONDITION         = """
			(a.aclId IS NULL
			 OR a.userId = :userId
			 OR EXISTS (SELECT 1 FROM WebSiteUser wsu WHERE wsu.id = :userId AND wsu.globalPermission = TRUE))
			""";
	/**
	 * Variant used by the full listing, which tests the file column itself instead of the
	 * joined row, exactly as the PHP repository did.
	 */
	String OWN_ACL_ACCESS_CONDITION = """
			(f.aclId IS NULL
			 OR a.userId = :userId
			 OR EXISTS (SELECT 1 FROM WebSiteUser wsu WHERE wsu.id = :userId AND wsu.globalPermission = TRUE))
			""";

	Optional<WebSiteFile> findByFileId(Long fileId);

	Optional<WebSiteFile> findByFilePath(String filePath);

	List<WebSiteFile> findByIdIn(Collection<Long> ids);

	@Query("SELECT f FROM WebSiteFile f LEFT JOIN WebSiteAcl a ON a.aclId = f.aclId WHERE " + OWN_ACL_ACCESS_CONDITION
	       + " ORDER BY f.id DESC")
	List<WebSiteFile> findAllFilesForUser(@Param("userId") long userId);

	@Query("SELECT f FROM WebSiteFile f LEFT JOIN WebSiteAcl a ON a.aclId = f.aclId WHERE " + ACCESS_CONDITION
	       + " AND f.filePath = :filePath")
	List<WebSiteFile> findByFilePathForUser(@Param("userId") long userId, @Param("filePath") String filePath);

	@Query(value = """
			SELECT f.* FROM web_site_file f
			JOIN web_site_gallery_file gf ON gf.file_id = f.id
			JOIN web_site_gallery g ON g.id = gf.gallery_id
			LEFT JOIN web_site_acl a ON a.acl_id = f.acl_id
			WHERE g.gallery_id = :galleryId
			  AND (a.acl_id IS NULL OR a.user_id = :userId
			       OR EXISTS (SELECT 1 FROM web_site_users u
			                  WHERE u.id = :userId AND u.global_permission = TRUE))
			ORDER BY f.file_path ASC, f.id ASC
			""", nativeQuery = true)
	List<WebSiteFile> findByGalleryIdForUser(@Param("galleryId") long galleryId,
	                                         @Param("userId") long userId);

	@Query("SELECT f FROM WebSiteFile f LEFT JOIN WebSiteAcl a ON a.aclId = f.aclId WHERE " + ACCESS_CONDITION
	       + " AND LOWER(f.mimetype) LIKE :mimePrefix"
	       + " ORDER BY f.originalDateTime DESC, f.id DESC")
	List<WebSiteFile> findLatestByMimePrefixForUser(@Param("userId") long userId,
	                                                @Param("mimePrefix") String mimePrefix,
	                                                Limit limit);

	/**
	 * Documents are everything that is not an image, video or audio file.
	 */
	@Query("SELECT f FROM WebSiteFile f LEFT JOIN WebSiteAcl a ON a.aclId = f.aclId WHERE " + ACCESS_CONDITION
	       + " AND LOWER(f.mimetype) NOT LIKE 'image/%'"
	       + " AND LOWER(f.mimetype) NOT LIKE 'video/%'"
	       + " AND LOWER(f.mimetype) NOT LIKE 'audio/%'"
	       + " ORDER BY f.originalDateTime DESC, f.id DESC")
	List<WebSiteFile> findLatestDocumentsForUser(@Param("userId") long userId, Limit limit);

	@Query("SELECT f FROM WebSiteFile f LEFT JOIN WebSiteAcl a ON a.aclId = f.aclId WHERE " + ACCESS_CONDITION
	       + " ORDER BY f.originalDateTime DESC, f.id DESC")
	List<WebSiteFile> findLatestForUser(@Param("userId") long userId, Limit limit);

	@Query(value = """
			SELECT id,
			       COALESCE(NULLIF(regexp_replace(file_path, '^.*/', ''), ''), file_path) AS title,
			       file_path AS filePath,
			       CASE WHEN original_datetime IS NULL THEN NULL
			            ELSE to_char(original_datetime, 'YYYY-MM-DD"T"HH24:MI:SS') END AS published
			FROM web_site_file
			WHERE original_datetime IS NOT NULL
			  AND LOWER(mimetype) LIKE 'image/%'
			  AND to_char(original_datetime::date, 'MM-DD') = to_char(CURRENT_DATE, 'MM-DD')
			ORDER BY random()
			LIMIT :maxResults
			""", nativeQuery = true)
	List<SameDateImage> findRandomImagesByCurrentMonthDay(@Param("maxResults") int maxResults);

	/**
	 * Same-date random image used by the {@code today_random} embed.
	 */
	interface SameDateImage {
		Long getId();

		String getTitle();

		String getFilePath();

		String getPublished();
	}
}
