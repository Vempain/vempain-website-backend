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
@Table(name = "web_site_page")
@Getter
public class WebSitePage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "page_id", nullable = false)
	private Long pageId;

	@Column(name = "acl_id")
	private Long aclId;

	@Column(name = "body")
	private String body;

	@Column(name = "page_style")
	private String pageStyle;

	@Column(name = "header", nullable = false, length = 512)
	private String header;

	@Column(name = "indexlist", nullable = false)
	private boolean indexList;

	@Column(name = "parent_id")
	private Long parentId;

	@Column(name = "file_path", nullable = false, length = 255)
	private String filePath;

	@Column(name = "secure", nullable = false)
	private boolean secure;

	@Column(name = "title", nullable = false, length = 512)
	private String title;

	@Column(name = "creator", nullable = false, length = 512)
	private String creator;

	@Column(name = "created", nullable = false)
	private LocalDateTime created;

	@Column(name = "modifier", length = 512)
	private String modifier;

	@Column(name = "modified")
	private LocalDateTime modified;

	@Column(name = "published")
	private LocalDateTime published;

	/**
	 * Rendered page content produced by the publisher; preferred over {@link #body}.
	 */
	@Setter
	@Column(name = "cache")
	private String cache;

	/**
	 * JSON array of the embeds discovered in the page content.
	 */
	@Setter
	@Column(name = "embeds")
	private String embeds;
}
