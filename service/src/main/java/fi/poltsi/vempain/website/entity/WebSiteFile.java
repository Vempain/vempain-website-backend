package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "web_site_file")
public class WebSiteFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "file_id", nullable = false)
	private Long fileId;

	@Column(name = "acl_id")
	private Long aclId;

	@Column(name = "comment")
	private String comment;

	@Column(name = "file_path", nullable = false, length = 512)
	private String filePath;

	@Column(name = "mimetype", nullable = false, length = 255)
	private String mimetype;

	@Column(name = "original_datetime")
	private OffsetDateTime originalDateTime;

	@Column(name = "rights_holder")
	private String rightsHolder;

	@Column(name = "rights_terms")
	private String rightsTerms;

	@Column(name = "rights_url")
	private String rightsUrl;

	@Column(name = "creator_name")
	private String creatorName;

	@Column(name = "creator_email")
	private String creatorEmail;

	@Column(name = "creator_country")
	private String creatorCountry;

	@Column(name = "creator_url")
	private String creatorUrl;

	@Column(name = "location_id")
	private Long locationId;

	@Column(name = "width")
	private Long width;

	@Column(name = "height")
	private Long height;

	@Column(name = "length")
	private Long length;

	@Column(name = "pages")
	private Long pages;

	/**
	 * Free form metadata, stored by the publisher as a JSON document.
	 */
	@Column(name = "metadata")
	private String metadata;

	@Column(name = "thumbnail_path")
	private String thumbnailPath;

	public Long getId() {
		return id;
	}

	public Long getFileId() {
		return fileId;
	}

	public Long getAclId() {
		return aclId;
	}

	public String getComment() {
		return comment;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getMimetype() {
		return mimetype;
	}

	public OffsetDateTime getOriginalDateTime() {
		return originalDateTime;
	}

	public String getRightsHolder() {
		return rightsHolder;
	}

	public String getRightsTerms() {
		return rightsTerms;
	}

	public String getRightsUrl() {
		return rightsUrl;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public String getCreatorEmail() {
		return creatorEmail;
	}

	public String getCreatorCountry() {
		return creatorCountry;
	}

	public String getCreatorUrl() {
		return creatorUrl;
	}

	public Long getLocationId() {
		return locationId;
	}

	public Long getWidth() {
		return width;
	}

	public Long getHeight() {
		return height;
	}

	public Long getLength() {
		return length;
	}

	public Long getPages() {
		return pages;
	}

	public String getMetadata() {
		return metadata;
	}

	public String getThumbnailPath() {
		return thumbnailPath;
	}
}
