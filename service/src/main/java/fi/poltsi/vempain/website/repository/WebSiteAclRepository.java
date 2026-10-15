package fi.poltsi.vempain.website.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fi.poltsi.vempain.website.entity.WebSiteAcl;

public interface WebSiteAclRepository extends JpaRepository<WebSiteAcl, WebSiteAcl.WebSiteAclId> {

    /**
     * An ACL identifier that has no rows at all does not restrict anything, which is how the PHP backend treated ACL identifiers of removed access lists.
     */
    boolean existsByAclId(Long aclId);

    boolean existsByAclIdAndUserId(Long aclId, Long userId);
}
