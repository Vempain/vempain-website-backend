package fi.poltsi.vempain.website.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fi.poltsi.vempain.website.entity.WebSiteJwtToken;

public interface WebSiteJwtTokenRepository extends JpaRepository<WebSiteJwtToken, Long> {

    @Query("SELECT t FROM WebSiteJwtToken t WHERE t.token = :token AND t.expiresAt > :now")
    Optional<WebSiteJwtToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Number of issued tokens that have not expired: the active visitor sessions.
     */
    @Query("SELECT COUNT(t) FROM WebSiteJwtToken t WHERE t.expiresAt > :now")
    long countValid(@Param("now") LocalDateTime now);

    /** Whether the exact token was already recorded; the same claims within one second sign to the same token */
    boolean existsByToken(String token);

    @Modifying
    @Query("DELETE FROM WebSiteJwtToken t WHERE t.token = :token")
    int deleteByToken(@Param("token") String token);

    /**
     * Ends the token at {@code expiresAt} unless it ends earlier already: a replaced token stays valid for a short grace period only.
     */
    @Modifying
    @Query("UPDATE WebSiteJwtToken t SET t.expiresAt = :expiresAt WHERE t.token = :token AND t.expiresAt > :expiresAt")
    int shortenExpiry(@Param("token") String token, @Param("expiresAt") LocalDateTime expiresAt);

    @Modifying
    @Query("DELETE FROM WebSiteJwtToken t WHERE t.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
