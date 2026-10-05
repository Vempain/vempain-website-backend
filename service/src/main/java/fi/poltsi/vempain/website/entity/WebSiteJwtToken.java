package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Server side record of an issued JWT. A token is only accepted while a matching,
 * non-expired row exists, which is what makes logout effective.
 */
@Entity
@Table(name = "web_site_jwt_token")
@Getter
@NoArgsConstructor
public class WebSiteJwtToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "token", nullable = false, length = 512)
	private String token;

	@Column(name = "creator", nullable = false)
	private Long creator;

	@Column(name = "created", nullable = false)
	private LocalDateTime created;

	@Column(name = "expires", nullable = false)
	private LocalDateTime expiresAt;

	public WebSiteJwtToken(Long userId, String token, LocalDateTime created, LocalDateTime expiresAt) {
		this.userId    = userId;
		this.token     = token;
		this.creator   = userId;
		this.created   = created;
		this.expiresAt = expiresAt;
	}
}
