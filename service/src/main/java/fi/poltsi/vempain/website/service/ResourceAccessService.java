package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.auth.AuthenticatedUser;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Turns an ACL decision into the HTTP status the PHP backend used: an anonymous caller
 * gets a 401 so that the frontend can offer a login, an authenticated caller who is not
 * on the list gets a 403.
 */
@Service
public class ResourceAccessService {

	private final AclService          aclService;
	private final CurrentUserProvider currentUserProvider;

	public ResourceAccessService(AclService aclService, CurrentUserProvider currentUserProvider) {
		this.aclService          = aclService;
		this.currentUserProvider = currentUserProvider;
	}

	public Optional<HttpStatus> deniedStatus(Long aclId) {
		if (aclId == null || aclId == 0L) {
			return Optional.empty();
		}

		AuthenticatedUser user = currentUserProvider.current()
		                                            .orElse(null);
		if (user != null && user.globalPermission()) {
			return Optional.empty();
		}

		if (aclService.canAccess(aclId, user)) {
			return Optional.empty();
		}

		return Optional.of(user == null ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN);
	}

	/**
	 * @throws ApiException when the caller may not see the resource
	 */
	public void requireAccess(Long aclId) {
		deniedStatus(aclId).ifPresent(status -> {
			throw new ApiException(status, status == HttpStatus.UNAUTHORIZED ? "Unauthorized" : "Forbidden");
		});
	}

	public boolean isAccessible(Long aclId) {
		return deniedStatus(aclId).isEmpty();
	}
}
