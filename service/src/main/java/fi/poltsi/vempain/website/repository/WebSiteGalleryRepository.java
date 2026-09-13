package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSiteGallery;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WebSiteGalleryRepository extends JpaRepository<WebSiteGallery, Long> {

	/**
	 * A gallery is visible when the left joined ACL produced no row, when the user is a
	 * member of its ACL, or when the user has the global permission flag.
	 */
	String ACCESS_CONDITION = """
			(a.aclId IS NULL
			 OR a.userId = :userId
			 OR EXISTS (SELECT 1 FROM WebSiteUser wsu WHERE wsu.id = :userId AND wsu.globalPermission = TRUE))
			""";

	Optional<WebSiteGallery> findByGalleryId(Long galleryId);

	List<WebSiteGallery> findByIdIn(Collection<Long> ids);

	@Query("SELECT g FROM WebSiteGallery g LEFT JOIN WebSiteAcl a ON a.aclId = g.aclId WHERE " + ACCESS_CONDITION
	       + " ORDER BY COALESCE(g.modified, g.created) DESC, g.id DESC")
	List<WebSiteGallery> findLatestAccessible(@Param("userId") long userId, Limit limit);

	@Query("SELECT g FROM WebSiteGallery g LEFT JOIN WebSiteAcl a ON a.aclId = g.aclId WHERE " + ACCESS_CONDITION
	       + " ORDER BY g.id ASC")
	List<WebSiteGallery> findAllAccessible(@Param("userId") long userId);
}
