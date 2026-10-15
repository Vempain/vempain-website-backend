package fi.poltsi.vempain.website.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fi.poltsi.vempain.website.entity.WebSiteUser;

public interface WebSiteUserRepository extends JpaRepository<WebSiteUser, Long> {

    Optional<WebSiteUser> findByUsername(String username);
}
