package fi.poltsi.vempain.website.traffic;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import fi.poltsi.vempain.website.config.SiteProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resolver trusts proxy headers only in the matching mode, takes the country from Cloudflare first and the GeoIP lookup second, and classifies bots.
 */
class ClientRequestResolverUTC {

    private static final String BROWSER = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36";

    private static ClientRequestResolver resolver(String mode, String trustedHeader, GeoIpCountryLookup lookup) {
        var properties = new SiteProperties();
        properties.getProxy()
            .setMode(mode);
        properties.getProxy()
            .setTrustedHeader(trustedHeader);
        return new ClientRequestResolver(properties, lookup);
    }

    private static GeoIpCountryLookup geoip(String country) {
        return new GeoIpCountryLookup(address -> Optional.ofNullable(country));
    }

    private static MockHttpServletRequest request(String remoteAddress, String userAgent) {
        var request = new MockHttpServletRequest("GET", "/api/pages/trips");
        request.setRemoteAddr(remoteAddress);
        if (userAgent != null) {
            request.addHeader("User-Agent", userAgent);
        }
        return request;
    }

    @Test
    void noneModeUsesTheSocketAddressAndIgnoresEveryProxyHeader() {
        var request = request("198.51.100.7", BROWSER);
        request.addHeader("CF-Connecting-IP", "203.0.113.9");
        request.addHeader("CF-IPCountry", "FI");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.11");

        var client = resolver("none", "", geoip("SE")).resolve(request);

        assertEquals("198.51.100.7", client.clientIp());
        assertEquals("socket", client.clientSource());
        assertEquals("SE", client.country(), "the spoofed Cloudflare country is ignored, the database decides");
        assertEquals(BROWSER, client.userAgent());
        assertFalse(client.bot());
    }

    @Test
    void cloudflareModeReadsTheConnectingIpAndTheCountryHeader() {
        var request = request("198.51.100.7", BROWSER);
        request.addHeader("CF-Connecting-IP", "203.0.113.9");
        request.addHeader("CF-IPCountry", "fi");

        var client = resolver("cloudflare", "", geoip("SE")).resolve(request);

        assertEquals("203.0.113.9", client.clientIp());
        assertEquals("cloudflare", client.clientSource());
        assertEquals("FI", client.country(), "the Cloudflare header wins over the database");
    }

    @Test
    void cloudflareModeWithoutHeadersFallsBackToTheSocketAndTheDatabase() {
        var request = request("198.51.100.7", BROWSER);
        request.addHeader("CF-IPCountry", "XX1");

        var client = resolver("cloudflare", "", geoip("SE")).resolve(request);

        assertEquals("198.51.100.7", client.clientIp());
        assertEquals("socket", client.clientSource());
        assertEquals("SE", client.country(), "an invalid country header is ignored");
    }

    @Test
    void forwardedModeTakesTheFirstForwardedAddressThenRealIpThenTheSocket() {
        var forwarded = request("10.0.0.2", BROWSER);
        forwarded.addHeader("X-Forwarded-For", " 203.0.113.10:4711 , 10.0.0.1");
        forwarded.addHeader("X-Real-IP", "203.0.113.11");
        var realIp = request("10.0.0.2", BROWSER);
        realIp.addHeader("X-Real-IP", "[2001:db8::1]:443");
        var bare = request("10.0.0.2", BROWSER);
        var resolver = resolver("forwarded", "", new GeoIpCountryLookup(address -> Optional.empty()));

        assertEquals("203.0.113.10", resolver.resolve(forwarded)
            .clientIp());
        assertEquals("forwarded", resolver.resolve(forwarded)
            .clientSource());
        assertEquals("2001:db8::1", resolver.resolve(realIp)
            .clientIp());
        assertEquals("10.0.0.2", resolver.resolve(bare)
            .clientIp());
        assertEquals("socket", resolver.resolve(bare)
            .clientSource());
        assertEquals(ClientRequest.UNKNOWN_COUNTRY, resolver.resolve(bare)
            .country());
    }

    @Test
    void aTrustedHeaderOverridesTheDefaultOfTheMode() {
        var request = request("10.0.0.2", BROWSER);
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        request.addHeader("True-Client-IP", "203.0.113.12");

        var client = resolver("forwarded", "True-Client-IP", new GeoIpCountryLookup(address -> Optional.empty())).resolve(request);

        assertEquals("203.0.113.12", client.clientIp());
    }

    @Test
    void unknownModeIsTreatedAsNone() {
        assertEquals(ProxyMode.NONE, ProxyMode.of("haproxy"));
        assertEquals(ProxyMode.NONE, ProxyMode.of(null));
        assertEquals(ProxyMode.CLOUDFLARE, ProxyMode.of(" CloudFlare "));
        assertEquals(ProxyMode.NONE, resolver("haproxy", "", geoip(null)).mode());
    }

    @Test
    void userAgentsAreTruncatedAndClassified() {
        var longAgent = "Mozilla/5.0 " + "x".repeat(1000);
        var client = resolver("none", "", geoip(null)).resolve(request("198.51.100.7", longAgent));

        assertEquals(ClientRequestResolver.USER_AGENT_MAX_LENGTH, client.userAgent()
            .length());
        assertTrue(ClientRequestResolver.isBot("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));
        assertTrue(ClientRequestResolver.isBot("curl/8.5.0"));
        assertTrue(ClientRequestResolver.isBot("python-requests/2.32"));
        assertTrue(ClientRequestResolver.isBot("facebookexternalhit/1.1"));
        assertTrue(ClientRequestResolver.isBot(null), "no user agent at all is not a browser");
        assertFalse(ClientRequestResolver.isBot(BROWSER));
        assertNull(resolver("none", "", geoip(null)).resolve(request("198.51.100.7", "  "))
            .userAgent());
        assertEquals("unknown", resolver("none", "", geoip(null)).resolve(request(null, BROWSER))
            .clientIp());
    }

    @Test
    void firstAddressHandlesEmptyAndOversizedValues() {
        assertNull(ClientRequestResolver.firstAddress(null));
        assertNull(ClientRequestResolver.firstAddress(" , 10.0.0.1"));
        assertNull(ClientRequestResolver.firstAddress("x".repeat(65)));
        assertNull(ClientRequestResolver.firstAddress("[2001:db8::1"));
        assertEquals("2001:db8::1", ClientRequestResolver.firstAddress("2001:db8::1"));
        assertEquals("203.0.113.9", ClientRequestResolver.firstAddress("203.0.113.9:8080"));
    }
}
