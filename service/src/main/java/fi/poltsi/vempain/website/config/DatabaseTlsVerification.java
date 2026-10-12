package fi.poltsi.vempain.website.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Refuses to start in the prod profile when the datasource URL does not make the PostgreSQL driver insist on TLS, so a production
 * deployment can never talk to the site database in plaintext.
 */
@Component
@Profile("prod")
public class DatabaseTlsVerification {

	private static final Set<String> TLS_SSL_MODES = Set.of("require", "verify-ca", "verify-full");

	public DatabaseTlsVerification(Environment environment) {
		if (!requiresTls(environment.getProperty("spring.datasource.url", ""))) {
			throw new IllegalStateException("Production datasource URL must require TLS: set sslmode=require, verify-ca or verify-full");
		}
	}

	/**
	 * Tells whether a PostgreSQL JDBC URL makes the driver insist on TLS: {@code sslmode} is require, verify-ca or verify-full, or {@code ssl} is
	 * given without an {@code sslmode} (the driver then verifies the full certificate). Weaker modes such as prefer or allow fall back to
	 * plaintext when the server offers no TLS, so they do not count.
	 */
	static boolean requiresTls(String jdbcUrl) {
		var queryStart = jdbcUrl == null ? -1 : jdbcUrl.indexOf('?');
		if (queryStart < 0) {
			return false;
		}

		String sslMode = null;
		var    ssl     = false;
		for (var parameter : jdbcUrl.substring(queryStart + 1)
									.split("&")) {
			var separator = parameter.indexOf('=');
			var name = (separator < 0 ? parameter : parameter.substring(0, separator)).trim()
																					  .toLowerCase(Locale.ROOT);
			var value = separator < 0 ? "" : URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8)
													   .trim()
													   .toLowerCase(Locale.ROOT);
			if ("sslmode".equals(name)) {
				sslMode = value;
			} else if ("ssl".equals(name)) {
				ssl = !"false".equals(value);
			}
		}

		return sslMode == null ? ssl : TLS_SSL_MODES.contains(sslMode);
	}
}
