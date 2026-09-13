package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebSiteUserRepository extends JpaRepository<WebSiteUser, Long> {

	Optional<WebSiteUser> findByUsername(String username);
}
