package fi.poltsi.vempain.website.traffic;

import java.util.Locale;

/**
 * Where the real client address of a request comes from. Only the headers of the configured mode are read; in {@link #NONE} every proxy header is ignored,
 * because a client can send them itself when no proxy in front of the service overwrites them.
 */
public enum ProxyMode {

    /**
     * The socket address is the client.
     */
    NONE("socket", null),
    /**
     * Cloudflare in front: {@code CF-Connecting-IP} carries the client, {@code CF-IPCountry} its country.
     */
    CLOUDFLARE("cloudflare", "CF-Connecting-IP"),
    /**
     * Any reverse proxy setting {@code X-Forwarded-For} (first address) or {@code X-Real-IP}.
     */
    FORWARDED("forwarded", "X-Forwarded-For");

    private final String source;
    private final String defaultHeader;

    ProxyMode(String source, String defaultHeader) {
        this.source = source;
        this.defaultHeader = defaultHeader;
    }

    /**
     * The {@code client_source} value of the access log for addresses resolved in this mode.
     */
    public String source() {
        return source;
    }

    public String defaultHeader() {
        return defaultHeader;
    }

    /**
     * Parses the configured mode; an unknown or blank value is {@link #NONE}, the safe choice.
     */
    public static ProxyMode of(String value) {
        if (value == null
            || value.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(value.trim()
                .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
