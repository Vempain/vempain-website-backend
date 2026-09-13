package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "web_site_gallery")
public class WebSiteGallery {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * Identifier used by the embed tags in the page content.
	 */
	@Column(name = "gallery_id", nullable = false)
	private Long galleryId;

	@Column(name = "acl_id")
	private Long aclId;

	@Column(name = "shortname", length = 255)
	private String shortname;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "creator", nullable = false)
	private Long creator;

	@Column(name = "created", nullable = false)
	private LocalDateTime created;

	@Column(name = "modifier")
	private Long modifier;

	@Column(name = "modified")
	private LocalDateTime modified;

	public Long getId() {
		return id;
	}

	public Long getGalleryId() {
		return galleryId;
	}

	public Long getAclId() {
		return aclId;
	}

	public String getShortname() {
		return shortname;
	}

	public String getDescription() {
		return description;
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
}
