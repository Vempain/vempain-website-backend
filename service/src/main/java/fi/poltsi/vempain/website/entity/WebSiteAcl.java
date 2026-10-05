package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Access control list row. A resource referencing {@code acl_id} is visible to the
 * users listed here; a resource with no matching row at all is public.
 */
@Entity
@Table(name = "web_site_acl")
@IdClass(WebSiteAcl.WebSiteAclId.class)
@Getter
public class WebSiteAcl {

	@Id
	@Column(name = "acl_id", nullable = false)
	private Long aclId;

	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	public static class WebSiteAclId implements Serializable {

		private Long aclId;
		private Long userId;
	}
}
