package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.auth.AuthenticatedUser;
import fi.poltsi.vempain.website.auth.JwtService;
import fi.poltsi.vempain.website.repository.WebSiteAclRepository;
import org.springframework.stereotype.Service;

@Service
public class AclService {

	private final WebSiteAclRepository aclRepository;
	private final JwtService           jwtService;

	public AclService(WebSiteAclRepository aclRepository, JwtService jwtService) {
		this.aclRepository = aclRepository;
		this.jwtService    = jwtService;
	}

	/**
	 * Decides whether the caller may see a resource protected by the given ACL. In
	 * addition to the ACL membership the token is verified against the persisted tokens,
	 * so a token that was invalidated by a logout no longer grants access.
	 */
	public boolean canAccess(Long aclId, AuthenticatedUser user) {
		if (!aclRequiresAuth(aclId)) {
			return true;
		}

		if (user == null || user.userId() <= 0L || user.token() == null) {
			return false;
		}

		Long tokenOwner = jwtService.tokenOwner(user.token())
		                            .orElse(null);
		if (tokenOwner == null || tokenOwner != user.userId()) {
			return false;
		}

		return aclRepository.existsByAclIdAndUserId(aclId, user.userId());
	}

	/**
	 * An ACL identifier of {@code null} or {@code 0} marks public content, and an
	 * identifier without any rows no longer restricts anything.
	 */
	public boolean aclRequiresAuth(Long aclId) {
		if (aclId == null || aclId == 0L) {
			return false;
		}

		return aclRepository.existsByAclId(aclId);
	}
}
