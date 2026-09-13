package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * Access control list row. A resource referencing {@code acl_id} is visible to the
 * users listed here; a resource with no matching row at all is public.
 */
@Entity
@Table(name = "web_site_acl")
@IdClass(WebSiteAcl.WebSiteAclId.class)
public class WebSiteAcl {

	@Id
	@Column(name = "acl_id", nullable = false)
	private Long aclId;

	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	public Long getAclId() {
		return aclId;
	}

	public Long getUserId() {
		return userId;
	}

	public static class WebSiteAclId implements Serializable {

		private Long aclId;
		private Long userId;

		public WebSiteAclId() {
		}

		public WebSiteAclId(Long aclId, Long userId) {
			this.aclId  = aclId;
			this.userId = userId;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof WebSiteAclId that)) {
				return false;
			}
			return Objects.equals(aclId, that.aclId) && Objects.equals(userId, that.userId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(aclId, userId);
		}
	}
}
