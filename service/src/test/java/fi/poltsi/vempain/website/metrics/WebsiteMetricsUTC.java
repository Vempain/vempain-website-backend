package fi.poltsi.vempain.website.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import fi.poltsi.vempain.website.exception.ApiException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebsiteMetricsUTC {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final WebsiteMetrics metrics = new WebsiteMetrics(registry);

    @Test
    void resourceRequestsAreCountedByResultAndServedBytesSummed() {
        metrics.resourceServed(1024);
        metrics.resourceServed(16);
        metrics.resourceRequest(WebsiteMetrics.RESULT_DENIED);
        metrics.resourceRequest(WebsiteMetrics.RESULT_NOT_FOUND);

        assertEquals(2.0, registry.get("vempain.site.resource.requests")
            .tag("result", "served")
            .counter()
            .count());
        assertEquals(1.0, registry.get("vempain.site.resource.requests")
            .tag("result", "denied")
            .counter()
            .count());
        assertEquals(1040.0, registry.get("vempain.site.resource.bytes")
            .counter()
            .count());
    }

    @Test
    void pageViewsFallBackToTheUnknownPath() {
        metrics.pageView("trips/summer");
        metrics.pageView("trips/summer");
        metrics.pageView(null);
        metrics.pageView(" ");

        assertEquals(2.0, registry.get("vempain.site.page.views")
            .tag("path", "trips/summer")
            .counter()
            .count());
        assertEquals(2.0, registry.get("vempain.site.page.views")
            .tag("path", WebsiteMetrics.UNKNOWN_PATH)
            .counter()
            .count());
    }

    @Test
    void loginsRevocationsAndSearchesAreRecorded() {
        metrics.login(WebsiteMetrics.RESULT_SUCCESS);
        metrics.login(WebsiteMetrics.RESULT_INVALID_CREDENTIALS);
        metrics.tokenRevoked();
        var result = metrics.timeSearch(() -> "found");
        metrics.searchResults(7);

        assertEquals("found", result);
        assertEquals(1.0, registry.get("vempain.site.logins")
            .tag("result", "invalid_credentials")
            .counter()
            .count());
        assertEquals(1.0, registry.get("vempain.site.tokens.revoked")
            .counter()
            .count());
        assertEquals(1, registry.get("vempain.site.search.duration")
            .timer()
            .count());
        assertEquals(7.0, registry.get("vempain.site.search.results")
            .summary()
            .totalAmount());
    }

    @Test
    void embedRequestsAreCountedByTypeAndOutcome() {
        assertEquals("music", metrics.embedRequest("music", () -> "music"));
        assertThrows(ApiException.class, () -> metrics.embedRequest("gps_track", () -> {
            throw new ApiException(HttpStatus.NOT_FOUND, "missing");
        }));
        assertThrows(IllegalStateException.class, () -> metrics.embedRequest("gps_track", () -> {
            throw new IllegalStateException("boom");
        }));
        metrics.openGraphRequest("html", WebsiteMetrics.RESULT_OK);

        assertEquals(1.0, registry.get("vempain.site.embed.requests")
            .tags("type", "music", "result", "ok")
            .counter()
            .count());
        assertEquals(1.0, registry.get("vempain.site.embed.requests")
            .tags("type", "gps_track", "result", "not_found")
            .counter()
            .count());
        assertEquals(1.0, registry.get("vempain.site.embed.requests")
            .tags("type", "gps_track", "result", "error")
            .counter()
            .count());
        assertEquals(1.0, registry.get("vempain.site.opengraph.requests")
            .tags("format", "html", "result", "ok")
            .counter()
            .count());
    }

    @Test
    void accessRequestsAreCountedByStatusClassMethodSourceAndBotAndTimed() {
        metrics.accessRequest("2xx", "GET", "cloudflare", false, 12_000_000L);
        metrics.accessRequest("2xx", "GET", "cloudflare", false, 8_000_000L);
        metrics.accessRequest("5xx", "POST", "socket", true, 1_000_000L);
        metrics.accessCountry("FI");
        metrics.accessCountry(null);
        metrics.accessCountry(" ");

        assertEquals(2.0, registry.get("vempain.site.access.requests")
            .tags("status_class", "2xx", "method", "GET", "client_source", "cloudflare", "bot", "false")
            .counter()
            .count());
        assertEquals(1.0, registry.get("vempain.site.access.requests")
            .tags("status_class", "5xx", "method", "POST", "client_source", "socket", "bot", "true")
            .counter()
            .count());
        assertEquals(2, registry.get("vempain.site.access.duration")
            .tag("status_class", "2xx")
            .timer()
            .count());
        assertEquals(1.0, registry.get("vempain.site.access.country")
            .tag("country", "FI")
            .counter()
            .count());
        assertEquals(2.0, registry.get("vempain.site.access.country")
            .tag("country", "unknown")
            .counter()
            .count());
    }
}
