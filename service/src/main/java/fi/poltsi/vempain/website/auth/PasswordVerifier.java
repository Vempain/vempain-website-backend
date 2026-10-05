package fi.poltsi.vempain.website.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Verifies the password hashes written by the Vempain admin backend. Those are bcrypt
 * hashes produced by the PHP native password hasher, so only bcrypt is accepted here.
 */
@Component
@Slf4j
public class PasswordVerifier {

	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	public boolean matches(String rawPassword, String storedHash) {
		if (rawPassword == null || rawPassword.isEmpty() || storedHash == null || storedHash.isEmpty()) {
			return false;
		}

		if (!storedHash.startsWith("$2")) {
			log.warn("Stored password hash uses an unsupported scheme, refusing the login");
			return false;
		}

		return bcrypt.matches(rawPassword, storedHash);
	}
}
