package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "web_site_users")
public class WebSiteUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "username", nullable = false, length = 255)
	private String username;

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

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Long getCreator() {
		return creator;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public Long getModifier() {
		return modifier;
	}

	public LocalDateTime getModified() {
		return modified;
	}

	public boolean isGlobalPermission() {
		return globalPermission;
	}
}
