package fi.poltsi.vempain.website.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.poltsi.vempain.website.repository.WebSiteJwtTokenRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * State gauges read from the site schema: the active visitor sessions and the age of the newest publish. Each value is cached for a minute so that an export
 * never costs more than one query per gauge per minute, whatever the export interval.
 */
@Component
public class WebsiteGauges {

    static final Duration REFRESH_INTERVAL = Duration.ofMinutes(1);

    private final Clock clock;
    private final CachedValue activeTokens;
    private final CachedValue lastPublishAgeSeconds;

    @Autowired
    public WebsiteGauges(MeterRegistry registry, WebSiteJwtTokenRepository tokens, WebSitePageRepository pages) {
        this(registry, tokens, pages, Clock.systemUTC());
    }

    WebsiteGauges(MeterRegistry registry, WebSiteJwtTokenRepository tokens, WebSitePageRepository pages, Clock clock) {
        this.clock = clock;
        this.activeTokens = new CachedValue(() -> (double) tokens.countValid(LocalDateTime.now(clock)));
        this.lastPublishAgeSeconds = new CachedValue(() -> lastPublishAge(pages.findLatestPublished()));
        Gauge.builder("vempain.site.tokens.active", activeTokens, CachedValue::get)
            .description("Visitor session tokens that have not expired")
            .register(registry);
        Gauge.builder("vempain.site.last_publish.age.seconds", lastPublishAgeSeconds, CachedValue::get)
            .description("Seconds since the newest page was published to the site schema; NaN when nothing is published")
            .baseUnit("seconds")
            .register(registry);
    }

    private double lastPublishAge(LocalDateTime published) {
        if (published == null) {
            return Double.NaN;
        }
        var publishedAt = published.atZone(ZoneId.systemDefault())
            .toInstant();
        return Math.max(0L, Duration.between(publishedAt, Instant.now(clock))
            .toSeconds());
    }

    /**
     * A value refreshed from its source at most once per {@link #REFRESH_INTERVAL}; a failing source keeps the previous value.
     */
    private final class CachedValue {

        private final Supplier<Double> source;
        private volatile double value = Double.NaN;
        private volatile Instant refreshed;

        private CachedValue(Supplier<Double> source) {
            this.source = source;
        }

        double get() {
            var now = Instant.now(clock);
            if (refreshed == null
                || Duration.between(refreshed, now)
                    .compareTo(REFRESH_INTERVAL) >= 0) {
                try {
                    value = source.get();
                } catch (RuntimeException e) {
                    // The gauge keeps its last value; the next export retries
                }
                refreshed = now;
            }
            return value;
        }
    }
}
