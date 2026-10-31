package fi.poltsi.vempain.website.traffic;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ServerHttpObservationFilter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fi.poltsi.vempain.website.auth.AuthenticatedUser;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.test.simple.SimpleTracer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One JSON line per request with the agreed field names, the request id on the response, the health probe skipped, the meters incremented.
 */
class AccessLogFilterUTC {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00.123Z");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final WebsiteMetrics metrics = new WebsiteMetrics(registry);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();
    private Logger accessLogger;

    @BeforeEach
    void captureAccessLog() {
        accessLogger = (Logger) LoggerFactory.getLogger(AccessLogFilter.LOGGER_NAME);
        lines.start();
        accessLogger.addAppender(lines);
    }

    @AfterEach
    void releaseAccessLog() {
        accessLogger.detachAppender(lines);
    }

    private AccessLogFilter filter(boolean enabled, String mode) {
        var properties = new SiteProperties();
        properties.getAccessLog()
            .setEnabled(enabled);
        properties.getProxy()
            .setMode(mode);
        var resolver = new ClientRequestResolver(properties, new GeoIpCountryLookup((GeoIpCountryLookup.CountrySource) address -> Optional.of("SE")));
        return new AccessLogFilter(properties, resolver, metrics, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static MockHttpServletRequest request(String method, String path) {
        var request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) Firefox/133.0");
        return request;
    }

    private Map<String, Object> onlyLine() {
        assertEquals(1, lines.list.size(), "exactly one access line");
        return objectMapper.readValue(lines.list.getFirst()
            .getFormattedMessage(), new TypeReference<Map<String, Object>>() {
            });
    }

    @Test
    void aCompletedRequestIsLoggedWithEveryField() throws Exception {
        var request = request("GET", "/api/pages/trips");
        request.setQueryString("page=2");
        request.addHeader("Referer", "https://example.org/");
        request.addHeader("CF-Connecting-IP", "203.0.113.9");
        request.addHeader("CF-IPCountry", "FI");
        request.setAttribute(CurrentUserProvider.REQUEST_ATTRIBUTE, new AuthenticatedUser(7, "alice", false, "token"));
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {

            @Override
            protected void doGet(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) throws IOException {
                res.setStatus(200);
                res.getOutputStream()
                    .write("{\"pages\":[]}".getBytes());
            }
        });

        filter(true, "cloudflare").doFilter(request, response, chain);

        var requestId = response.getHeader(AccessLogFilter.REQUEST_ID_HEADER);
        assertNotNull(requestId);
        var line = onlyLine();
        assertEquals(
                List.of("ts", "type", "method", "path", "query", "status", "duration_ms", "bytes", "client_ip", "client_source", "country", "city", "latitude",
                        "longitude", "user_agent", "referer", "bot", "user", "request_id", "trace_id", "span_id"),
                List.copyOf(line.keySet()), "the agreed field names in order");
        assertEquals("2026-10-19T10:00:00.123Z", line.get("ts"));
        assertEquals("access", line.get("type"));
        assertEquals("GET", line.get("method"));
        assertEquals("/api/pages/trips", line.get("path"));
        assertEquals("page=2", line.get("query"));
        assertEquals(200, line.get("status"));
        assertTrue(((Number) line.get("duration_ms")).longValue() >= 0);
        assertEquals(12, line.get("bytes"));
        assertEquals("203.0.113.9", line.get("client_ip"));
        assertEquals("cloudflare", line.get("client_source"));
        assertEquals("FI", line.get("country"));
        assertEquals("Mozilla/5.0 (X11; Linux x86_64) Firefox/133.0", line.get("user_agent"));
        assertEquals("https://example.org/", line.get("referer"));
        assertEquals(false, line.get("bot"));
        assertEquals("alice", line.get("user"));
        assertEquals(requestId, line.get("request_id"));
        assertNull(line.get("trace_id"), "no tracer, no trace id");
        assertNull(line.get("span_id"));

        assertEquals(1.0, registry.get("vempain.site.access.requests")
            .tags("status_class", "2xx", "method", "GET", "client_source", "cloudflare", "bot", "false")
            .counter()
            .count());
        assertEquals(1, registry.get("vempain.site.access.duration")
            .tag("status_class", "2xx")
            .timer()
            .count());
        assertEquals(1.0, registry.get("vempain.site.access.country")
            .tag("country", "FI")
            .counter()
            .count());
    }

    @Test
    void anonymousRequestsWithoutBodyHaveNullsAndFallBackToTheContentLength() throws Exception {
        var request = request("HEAD", "/file/photo.jpg");
        var response = new MockHttpServletResponse();
        response.setHeader("Content-Length", "4096");

        filter(true, "none").doFilter(request, response, new MockFilterChain());

        var line = onlyLine();
        assertNull(line.get("query"));
        assertNull(line.get("referer"));
        assertNull(line.get("user"));
        assertEquals(4096, line.get("bytes"));
        assertEquals("198.51.100.7", line.get("client_ip"));
        assertEquals("socket", line.get("client_source"));
        assertEquals("SE", line.get("country"), "without a proxy header the database answers");
    }

    @Test
    void aMissingContentLengthGivesNullBytes() throws Exception {
        var response = new MockHttpServletResponse();

        filter(true, "none").doFilter(request("GET", "/api/x"), response, new MockFilterChain());

        assertNull(onlyLine().get("bytes"));
    }

    @Test
    void theHealthProbeAndADisabledLogWriteNothing() throws Exception {
        var health = request("GET", "/health");
        var healthResponse = new MockHttpServletResponse();
        filter(true, "none").doFilter(health, healthResponse, new MockFilterChain());
        assertNull(healthResponse.getHeader(AccessLogFilter.REQUEST_ID_HEADER));

        var disabledResponse = new MockHttpServletResponse();
        filter(false, "none").doFilter(request("GET", "/api/pages"), disabledResponse, new MockFilterChain());
        assertNull(disabledResponse.getHeader(AccessLogFilter.REQUEST_ID_HEADER));

        assertTrue(lines.list.isEmpty());
        assertNull(registry.find("vempain.site.access.requests")
            .counter());
    }

    @Test
    void aFailingRequestIsLoggedAsServerErrorAndTheExceptionPropagates() {
        var chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {

            @Override
            protected void doGet(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) throws ServletException {
                throw new ServletException("boom");
            }
        });
        var filter = filter(true, "none");

        assertThrows(ServletException.class, () -> filter.doFilter(request("GET", "/api/boom"), new MockHttpServletResponse(), chain));

        assertEquals(500, onlyLine().get("status"));
        assertEquals(1.0, registry.get("vempain.site.access.requests")
            .tag("status_class", "5xx")
            .counter()
            .count());
    }

    @Test
    void botsAndUnusualMethodsAreTaggedAsSuch() throws Exception {
        var request = new MockHttpServletRequest("PROPFIND", "/dav");
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("User-Agent", "curl/8.5.0");
        var response = new MockHttpServletResponse();
        response.setStatus(405);

        filter(true, "none").doFilter(request, response, new MockFilterChain());

        var line = onlyLine();
        assertEquals(true, line.get("bot"));
        assertEquals("PROPFIND", line.get("method"), "the line keeps the real method");
        assertEquals(1.0, registry.get("vempain.site.access.requests")
            .tags("status_class", "4xx", "method", "other", "bot", "true")
            .counter()
            .count(), "the meter bounds it to a known set");
        assertEquals("other", AccessLogFilter.statusClass(999));
        assertEquals("1xx", AccessLogFilter.statusClass(101));
        assertFalse(lines.list.getFirst()
            .getFormattedMessage()
            .contains("\n"), "one line");
    }

    /**
     * The server span is opened by Spring's observation filter after this filter and closed before the chain returns, so the ids come from the observation
     * context that filter leaves on the request.
     */
    @Test
    void theTraceAndSpanOfTheRequestAreTakenFromTheObservationContext() throws Exception {
        var tracer = new SimpleTracer();
        var properties = new SiteProperties();
        var resolver = new ClientRequestResolver(properties, new GeoIpCountryLookup((GeoIpCountryLookup.CountrySource) address -> Optional.empty()));
        var filter = new AccessLogFilter(properties, resolver, metrics, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC), tracer);
        var request = request("GET", "/api/pages");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {

            @Override
            protected void doGet(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) {
                // What ServerHttpObservationFilter leaves behind: the observation context with the (already ended) server span
                var span = tracer.nextSpan()
                    .name("http get")
                    .start();
                span.end();
                var tracingContext = new TracingObservationHandler.TracingContext();
                tracingContext.setSpan(span);
                var context = new ServerRequestObservationContext(req, res);
                context.put(TracingObservationHandler.TracingContext.class, tracingContext);
                req.setAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, context);
                res.setStatus(200);
            }
        });

        filter.doFilter(request, response, chain);

        var line = onlyLine();
        var span = tracer.onlySpan();
        assertEquals(span.context()
            .traceId(), line.get("trace_id"));
        assertEquals(span.context()
            .spanId(), line.get("span_id"));
    }

    @Test
    void withATracerButNoSpanTheIdsStayNull() throws Exception {
        var tracer = new SimpleTracer();
        var properties = new SiteProperties();
        var resolver = new ClientRequestResolver(properties, new GeoIpCountryLookup((GeoIpCountryLookup.CountrySource) address -> Optional.empty()));
        var filter = new AccessLogFilter(properties, resolver, metrics, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC), tracer);
        var response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/pages"), response, new MockFilterChain());

        var line = onlyLine();
        assertNull(line.get("trace_id"));
        assertNull(line.get("span_id"));
    }
}
