package fi.poltsi.vempain.website.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime configuration of the website backend. The property defaults are bound in application.yml to the same environment variables the PHP backend used, so
 * that an existing deployment can be switched over without changing the compose environment.
 */
@ConfigurationProperties(prefix = "vempain.site")
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

    /**
     * How the real client address is found when the service runs behind a reverse proxy.
     */
    private Proxy proxy = new Proxy();

    /**
     * Optional MaxMind GeoLite2 City lookup of the access log.
     */
    private Geoip geoip = new Geoip();

    /**
     * The structured access log written for every request.
     */
    private AccessLog accessLog = new AccessLog();

    public String getFilesRoot() {
        return filesRoot;
    }

    public void setFilesRoot(String filesRoot) {
        this.filesRoot = filesRoot;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getJwtTtlSeconds() {
        return jwtTtlSeconds;
    }

    public void setJwtTtlSeconds(long jwtTtlSeconds) {
        this.jwtTtlSeconds = jwtTtlSeconds;
    }

    public String getJwtCookieName() {
        return jwtCookieName;
    }

    public void setJwtCookieName(String jwtCookieName) {
        this.jwtCookieName = jwtCookieName;
    }

    public boolean isJwtCookieSecure() {
        return jwtCookieSecure;
    }

    public void setJwtCookieSecure(boolean jwtCookieSecure) {
        this.jwtCookieSecure = jwtCookieSecure;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Geoip getGeoip() {
        return geoip;
    }

    public void setGeoip(Geoip geoip) {
        this.geoip = geoip;
    }

    public AccessLog getAccessLog() {
        return accessLog;
    }

    public void setAccessLog(AccessLog accessLog) {
        this.accessLog = accessLog;
    }

    /**
     * Reverse proxy settings: which header, if any, carries the real client address. Headers are trusted only in the matching mode, because a client can send
     * them itself when nothing in front of the service overwrites them.
     */
    public static class Proxy {

        /**
         * {@code none}: the socket address is the client. {@code cloudflare}: {@code CF-Connecting-IP} and {@code CF-IPCountry}. {@code forwarded}: the first
         * address of {@code X-Forwarded-For}, falling back to {@code X-Real-IP}.
         */
        private String mode = "none";

        /**
         * Header name overriding the default header of the mode; empty keeps the default.
         */
        private String trustedHeader = "";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getTrustedHeader() {
            return trustedHeader;
        }

        public void setTrustedHeader(String trustedHeader) {
            this.trustedHeader = trustedHeader;
        }
    }

    public static class Geoip {

        /**
         * Path of a MaxMind GeoLite2-City {@code .mmdb}; empty disables the lookup.
         */
        private String database = "";

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }
    }

    public static class AccessLog {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
