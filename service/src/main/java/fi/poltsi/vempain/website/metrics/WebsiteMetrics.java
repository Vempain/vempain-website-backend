package fi.poltsi.vempain.website.metrics;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import fi.poltsi.vempain.website.exception.ApiException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * The event meters of the public site: resource serving, page views, visitor logins, searches, embeds and Open Graph previews. Every tag value is one of a
 * small fixed set (a result or a type); the page path is the only free value and the caller limits it to pages that exist.
 */
@Component
public class WebsiteMetrics {

    public static final String RESULT_SERVED = "served";
    public static final String RESULT_NOT_FOUND = "not_found";
    public static final String RESULT_DENIED = "denied";
    public static final String RESULT_OK = "ok";
    public static final String RESULT_ERROR = "error";
    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_INVALID_CREDENTIALS = "invalid_credentials";
    public static final String RESULT_BAD_REQUEST = "bad_request";
    public static final String UNKNOWN_PATH = "unknown";

    private final MeterRegistry registry;
    private final Counter resourceBytes;
    private final Counter tokensRevoked;
    private final Timer searchDuration;
    private final DistributionSummary searchResults;

    public WebsiteMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.resourceBytes = Counter.builder("vempain.site.resource.bytes")
            .description("Bytes of published files served")
            .baseUnit("bytes")
            .register(registry);
        this.tokensRevoked = Counter.builder("vempain.site.tokens.revoked")
            .description("Visitor sessions ended by logout")
            .register(registry);
        this.searchDuration = Timer.builder("vempain.site.search.duration")
            .description("Subject searches")
            .register(registry);
        this.searchResults = DistributionSummary.builder("vempain.site.search.results")
            .description("Matches (pages, galleries and files) of a subject search")
            .register(registry);
    }

    public void resourceRequest(String result) {
        registry.counter("vempain.site.resource.requests", "result", result)
            .increment();
    }

    public void resourceServed(long bytes) {
        resourceRequest(RESULT_SERVED);
        resourceBytes.increment(bytes);
    }

    /**
     * @param path the file path of an existing page, or {@link #UNKNOWN_PATH} so that the tag set stays bounded
     */
    public void pageView(String path) {
        registry.counter("vempain.site.page.views", "path", path == null
                                                            || path.isBlank() ? UNKNOWN_PATH : path)
            .increment();
    }

    public void login(String result) {
        registry.counter("vempain.site.logins", "result", result)
            .increment();
    }

    public void tokenRevoked() {
        tokensRevoked.increment();
    }

    public <T> T timeSearch(Supplier<T> search) {
        return searchDuration.record(search);
    }

    public void searchResults(long matches) {
        searchResults.record(matches);
    }

    /**
     * Runs one embed request and counts it by type and outcome: {@code ok}, the status name of an {@link ApiException} (for instance {@code not_found}) or
     * {@code error}.
     */
    public <T> T embedRequest(String type, Supplier<T> request) {
        try {
            var response = request.get();
            countEmbed(type, RESULT_OK);
            return response;
        } catch (ApiException e) {
            countEmbed(type, e.getStatus()
                .name()
                .toLowerCase());
            throw e;
        } catch (RuntimeException e) {
            countEmbed(type, RESULT_ERROR);
            throw e;
        }
    }

    private void countEmbed(String type, String result) {
        registry.counter("vempain.site.embed.requests", "type", type, "result", result)
            .increment();
    }

    public void openGraphRequest(String format, String result) {
        registry.counter("vempain.site.opengraph.requests", "format", format, "result", result)
            .increment();
    }

    /**
     * One completed request of the access log: the rate and error share by status class, and the latency. Every tag is one of a small fixed set; the client
     * address, user agent and path stay in the log line.
     *
     * @param statusClass  {@code 1xx} to {@code 5xx} or {@code other}
     * @param method       a known HTTP method or {@code other}
     * @param clientSource {@code socket}, {@code cloudflare} or {@code forwarded}
     */
    public void accessRequest(String statusClass, String method, String clientSource, boolean bot, long durationNanos) {
        registry
            .counter("vempain.site.access.requests", "status_class", statusClass, "method", method, "client_source", clientSource, "bot", Boolean.toString(bot))
            .increment();
        Timer.builder("vempain.site.access.duration")
            .description("Request duration by status class")
            .tag("status_class", statusClass)
            .register(registry)
            .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * @param country ISO 3166-1 alpha-2 code or {@code unknown}; bounded by the number of countries
     */
    public void accessCountry(String country) {
        registry.counter("vempain.site.access.country", "country", country == null
                                                                   || country.isBlank() ? "unknown" : country)
            .increment();
    }

    public void accessLocation(String country, String city, Double latitude, Double longitude) {
        if (latitude == null
            || longitude == null) {
            return;
        }
        registry
            .counter("vempain.site.access.locations", "country", value(country), "city", value(city), "latitude", latitude.toString(), "longitude",
                    longitude.toString())
            .increment();
    }

    private static String value(String value) {
        return value == null
               || value.isBlank() ? "unknown" : value;
    }
}
