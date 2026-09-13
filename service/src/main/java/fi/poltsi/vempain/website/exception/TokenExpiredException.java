package fi.poltsi.vempain.website.exception;

/**
 * Raised when a token is well formed and correctly signed but no longer valid. This is the
 * only token problem that is reported to the caller; anything else leaves the request
 * anonymous.
 */
public class TokenExpiredException extends RuntimeException {

	public TokenExpiredException(String message) {
		super(message);
	}
}
