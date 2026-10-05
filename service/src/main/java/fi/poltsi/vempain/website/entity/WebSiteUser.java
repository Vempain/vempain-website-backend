package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "web_site_users")
@Getter
public class WebSiteUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Setter
	@Column(name = "username", nullable = false, length = 255)
	private String username;

	@Setter
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "creator", nullable = false)
	private Long creator;

	@Column(name = "created", nullable = false)
	private LocalDateTime created;

	@Column(name = "modifier")
	private Long modifier;

	@Column(name = "modified")
	private LocalDateTime modified;

	@Column(name = "global_permission", nullable = false)
	private boolean globalPermission;
}
