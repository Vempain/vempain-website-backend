package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSiteJwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WebSiteJwtTokenRepository extends JpaRepository<WebSiteJwtToken, Long> {

	@Query("SELECT t FROM WebSiteJwtToken t WHERE t.token = :token AND t.expiresAt > :now")
	Optional<WebSiteJwtToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

	@Modifying
	@Query("DELETE FROM WebSiteJwtToken t WHERE t.token = :token")
	int deleteByToken(@Param("token") String token);

	@Modifying
	@Query("DELETE FROM WebSiteJwtToken t WHERE t.expiresAt <= :now")
	int deleteExpired(@Param("now") LocalDateTime now);
}
