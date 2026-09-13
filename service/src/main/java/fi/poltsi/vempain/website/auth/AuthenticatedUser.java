package fi.poltsi.vempain.website.auth;

/**
 * Claims of the caller as resolved from the JWT. The raw token is kept because the ACL
 * check verifies it against the tokens persisted in the database.
 *
 * @param userId           value of the {@code sub} claim
 * @param username         value of the {@code username} claim
 * @param globalPermission value of the {@code global_permission} claim
 * @param token            the raw, still encoded token
 */
public record AuthenticatedUser(long userId, String username, boolean globalPermission, String token) {

	/**
	 * Identifier used for anonymous callers, mirroring the PHP backend.
	 */
	public static final long ANONYMOUS_USER_ID = -1L;
}
