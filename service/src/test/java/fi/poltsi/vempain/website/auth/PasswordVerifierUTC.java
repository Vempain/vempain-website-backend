package fi.poltsi.vempain.website.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordVerifierUTC {

    private final PasswordVerifier verifier = new PasswordVerifier();
    private final String hash = new BCryptPasswordEncoder(4).encode("correct horse");

    @Test
    void bcryptHashesAreVerified() {
        assertTrue(verifier.matches("correct horse", hash));
        assertFalse(verifier.matches("wrong horse", hash));
    }

    @Test
    void missingInputNeverMatches() {
        assertFalse(verifier.matches(null, hash));
        assertFalse(verifier.matches("", hash));
        assertFalse(verifier.matches("correct horse", null));
        assertFalse(verifier.matches("correct horse", ""));
    }

    @Test
    void unsupportedHashSchemesAreRefused() {
        assertFalse(verifier.matches("correct horse", "{noop}correct horse"));
        assertFalse(verifier.matches("correct horse", "correct horse"));
    }
}
