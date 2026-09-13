package fi.poltsi.vempain.website.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Verifies the password hashes written by the Vempain admin backend. Those are bcrypt
 * hashes produced by the PHP native password hasher, so only bcrypt is accepted here.
 */
@Component
public class PasswordVerifier {

	private static final Logger LOG = LoggerFactory.getLogger(PasswordVerifier.class);

	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	public boolean matches(String rawPassword, String storedHash) {
		if (rawPassword == null || rawPassword.isEmpty() || storedHash == null || storedHash.isEmpty()) {
			return false;
		}

		if (!storedHash.startsWith("$2")) {
			LOG.warn("Stored password hash uses an unsupported scheme, refusing the login");
			return false;
		}

		return bcrypt.matches(rawPassword, storedHash);
	}
}
