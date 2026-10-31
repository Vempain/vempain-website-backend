package fi.poltsi.vempain.website.traffic;

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import fi.poltsi.vempain.website.config.SiteProperties;

/**
 * Resolves the client of a request for the access log: the real address according to the proxy mode, its country (Cloudflare's header first, then the optional
 * GeoIP database), the user agent and a rough bot classification. Proxy headers are read only in the matching mode.
 */
@Component
public class ClientRequestResolver {

    public static final int USER_AGENT_MAX_LENGTH = 512;
    static final String CLOUDFLARE_COUNTRY_HEADER = "CF-IPCountry";
    static final String REAL_IP_HEADER = "X-Real-IP";

    /**
     * Lower case fragments of user agents that are crawlers, previews or command line clients rather than browsers.
     */
    static final List<String> BOT_MARKERS = List.of("bot", "crawler", "spider", "slurp", "curl", "wget", "python-requests", "python-urllib", "go-http-client",
            "java/", "okhttp", "httpclient", "facebookexternalhit", "googlebot", "bingbot", "yandex", "duckduckbot", "baiduspider", "applebot", "semrush",
            "ahrefs", "mj12bot", "petalbot", "headlesschrome", "phantomjs", "lighthouse", "pingdom", "uptimerobot", "monitor");

    private final ProxyMode mode;
    private final String clientHeader;
    private final GeoIpCountryLookup countryLookup;

    public ClientRequestResolver(SiteProperties siteProperties, GeoIpCountryLookup countryLookup) {
        this.mode = ProxyMode.of(siteProperties.getProxy()
            .getMode());
        var trustedHeader = siteProperties.getProxy()
            .getTrustedHeader();
        this.clientHeader = trustedHeader == null
                            || trustedHeader.isBlank() ? mode.defaultHeader() : trustedHeader.trim();
        this.countryLookup = countryLookup;
    }

    public ProxyMode mode() {
        return mode;
    }

    public ClientRequest resolve(HttpServletRequest request) {
        var address = resolveAddress(request);
        var userAgent = userAgent(request);
        var location = location(request, address.ip());
        return new ClientRequest(address.ip(), address.source(), location.country(), location.city(), location.latitude(), location.longitude(), userAgent,
                isBot(userAgent));
    }

    private record Address(String ip, String source) {}

    private Address resolveAddress(HttpServletRequest request) {
        var socket = new Address(nullToUnknown(request.getRemoteAddr()), ProxyMode.NONE.source());
        if (mode == ProxyMode.NONE) {
            return socket;
        }

        var header = firstAddress(request.getHeader(clientHeader));
        if (header == null
            && mode == ProxyMode.FORWARDED) {
            header = firstAddress(request.getHeader(REAL_IP_HEADER));
        }
        return header == null ? socket : new Address(header, mode.source());
    }

    /**
     * The first (client-most) address of a comma separated proxy header, without a port suffix.
     */
    static String firstAddress(String headerValue) {
        if (headerValue == null
            || headerValue.isBlank()) {
            return null;
        }
        var first = headerValue.split(",", 2)[0].trim();
        if (first.isEmpty()
            || first.length() > 64) {
            return null;
        }
        // IPv4 with port ("203.0.113.9:4711"); bracketed IPv6 with port ("[2001:db8::1]:4711")
        if (first.startsWith("[")) {
            var end = first.indexOf(']');
            return end > 1 ? first.substring(1, end) : null;
        }
        var colon = first.indexOf(':');
        if (colon > 0
            && first.indexOf(':', colon + 1) < 0) {
            return first.substring(0, colon);
        }
        return first;
    }

    private GeoIpCountryLookup.GeoIpLocation location(HttpServletRequest request, String ip) {
        var location = countryLookup.location(ip)
            .orElse(GeoIpCountryLookup.GeoIpLocation.unknown());
        if (location.country() == null
            || location.country()
                .isBlank()) {
            location = location.withCountry(ClientRequest.UNKNOWN_COUNTRY);
        }
        if (mode == ProxyMode.CLOUDFLARE) {
            var header = request.getHeader(CLOUDFLARE_COUNTRY_HEADER);
            if (header != null
                && header.matches("[A-Za-z]{2}")) {
                return location.withCountry(header.toUpperCase(Locale.ROOT));
            }
        }
        return location;
    }

    private static String userAgent(HttpServletRequest request) {
        var header = request.getHeader("User-Agent");
        if (header == null
            || header.isBlank()) {
            return null;
        }
        var trimmed = header.strip();
        return trimmed.length() > USER_AGENT_MAX_LENGTH ? trimmed.substring(0, USER_AGENT_MAX_LENGTH) : trimmed;
    }

    static boolean isBot(String userAgent) {
        if (userAgent == null) {
            return true;
        }
        var lower = userAgent.toLowerCase(Locale.ROOT);
        return BOT_MARKERS.stream()
            .anyMatch(lower::contains);
    }

    private static String nullToUnknown(String value) {
        return value == null
               || value.isBlank() ? "unknown" : value;
    }
}
