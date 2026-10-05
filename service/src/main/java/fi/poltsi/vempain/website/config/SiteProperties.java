package fi.poltsi.vempain.website.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime configuration of the website backend. The property defaults are bound in
 * application.yml to the same environment variables the PHP backend used, so that an
 * existing deployment can be switched over without changing the compose environment.
 */
@ConfigurationProperties(prefix = "vempain.site")
@Getter
@Setter
public class SiteProperties {

	/**
	 * Root directory of the published files, served through /file/...
	 */
	private String filesRoot = "/files";

	/**
	 * Secret used to sign the HS256 JWT tokens.
	 */
	private String jwtSecret = "";

	/**
	 * Lifetime of an issued JWT in seconds.
	 */
	private long jwtTtlSeconds = 1200L;

	/**
	 * Name of the HttpOnly cookie carrying the JWT.
	 */
	private String jwtCookieName = "OXALATE_JWT_TOKEN";

	/**
	 * Whether the auth cookie is flagged Secure.
	 */
	private boolean jwtCookieSecure = true;

	/**
	 * Origins allowed by the CORS filter; an empty list means "*".
	 */
	private List<String> corsAllowedOrigins = new ArrayList<>();
}
