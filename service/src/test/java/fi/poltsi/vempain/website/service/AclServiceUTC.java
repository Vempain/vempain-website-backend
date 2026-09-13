package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.auth.AuthenticatedUser;
import fi.poltsi.vempain.website.auth.JwtService;
import fi.poltsi.vempain.website.repository.WebSiteAclRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AclServiceUTC {
    private final WebSiteAclRepository repository = mock(WebSiteAclRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AclService service = new AclService(repository, jwt);

    @Test void nullAndUnknownAclArePublic() {
        assertFalse(service.aclRequiresAuth(null));
        assertFalse(service.aclRequiresAuth(0L));
        when(repository.existsByAclId(9L)).thenReturn(false);
        assertFalse(service.aclRequiresAuth(9L));
        assertTrue(service.canAccess(9L, null));
    }

    @Test void validPersistedMemberCanAccess() {
        when(repository.existsByAclId(4L)).thenReturn(true);
        when(jwt.tokenOwner("t")).thenReturn(Optional.of(12L));
        when(repository.existsByAclIdAndUserId(4L, 12L)).thenReturn(true);
        assertTrue(service.canAccess(4L, new AuthenticatedUser(12, "u", false, "t")));
    }

    @Test void missingInvalidOrNonMemberIsDenied() {
        when(repository.existsByAclId(4L)).thenReturn(true);
        assertFalse(service.canAccess(4L, null));
        assertFalse(service.canAccess(4L, new AuthenticatedUser(0, "u", false, "t")));
        when(jwt.tokenOwner("t")).thenReturn(Optional.of(13L));
        assertFalse(service.canAccess(4L, new AuthenticatedUser(12, "u", false, "t")));
        when(jwt.tokenOwner("t")).thenReturn(Optional.of(12L));
        assertFalse(service.canAccess(4L, new AuthenticatedUser(12, "u", false, "t")));
    }
}
