package fi.poltsi.vempain.website.auth;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import fi.poltsi.vempain.website.config.SiteProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieWriterUTC {

    private final SiteProperties properties = new SiteProperties();
    private final AuthCookieWriter writer = new AuthCookieWriter(properties);

    AuthCookieWriterUTC() {
        properties.setJwtCookieName("SITE_JWT");
    }

    @Test
    void bearerHeaderTakesPrecedenceOverTheCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer  header-token ");
        request.setCookies(new Cookie("SITE_JWT", "cookie-token"));

        assertEquals("header-token", writer.readToken(request));
    }

    @Test
    void emptyBearerHeaderFallsBackToTheCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer   ");
        request.setCookies(new Cookie("OTHER", "ignored"), new Cookie("SITE_JWT", "cookie-token"));

        assertEquals("cookie-token", writer.readToken(request));
    }

    @Test
    void otherAuthorizationSchemesAndForeignCookiesAreIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic abc");
        request.setCookies(new Cookie("OTHER", "ignored"));

        assertNull(writer.readToken(request));
        assertNull(writer.readToken(new MockHttpServletRequest()));
    }

    @Test
    void writeSetsTheMirrorHeaderAndASecureHttpOnlyCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(response, "issued", 1200L);

        assertEquals("issued", response.getHeader("X-Auth-Token"));
        List<String> attributes = cookieAttributes(response);
        assertEquals("SITE_JWT=issued", attributes.get(0));
        assertTrue(attributes.containsAll(List.of("Path=/", "HttpOnly", "SameSite=Lax", "Max-Age=1200", "Secure")), attributes.toString());
    }

    @Test
    void insecureDeploymentsOmitTheSecureFlag() {
        properties.setJwtCookieSecure(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(response, "issued", 60L);

        List<String> attributes = cookieAttributes(response);
        assertEquals("SITE_JWT=issued", attributes.get(0));
        assertTrue(attributes.containsAll(List.of("Path=/", "HttpOnly", "SameSite=Lax", "Max-Age=60")), attributes.toString());
        assertFalse(attributes.contains("Secure"));
    }

    @Test
    void clearExpiresTheCookieWithoutExposingAToken() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.clear(response);

        assertNull(response.getHeader("X-Auth-Token"));
        List<String> attributes = cookieAttributes(response);
        assertEquals("SITE_JWT=", attributes.get(0));
        assertTrue(attributes.containsAll(List.of("Max-Age=0", "HttpOnly")), attributes.toString());
    }

    /**
     * The attributes of the single {@code Set-Cookie} header; the mock response re-renders the header, so the order is not asserted.
     */
    private static List<String> cookieAttributes(MockHttpServletResponse response) {
        List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertEquals(1, headers.size());

        return Arrays.stream(headers.get(0)
            .split(";"))
            .map(String::trim)
            .toList();
    }
}
