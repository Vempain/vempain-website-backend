package fi.poltsi.vempain.website.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.website.repository.WebSiteJwtTokenRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The gauges read the site schema at most once a minute and keep their last value when the source fails.
 */
class WebsiteGaugesUTC {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final WebSiteJwtTokenRepository tokens = mock(WebSiteJwtTokenRepository.class);
    private final WebSitePageRepository pages = mock(WebSitePageRepository.class);
    private final Instant start = Instant.parse("2026-10-19T12:00:00Z");
    private final MutableClock clock = new MutableClock(start);

    @Test
    void valuesAreCachedForAMinute() {
        when(tokens.countValid(any())).thenReturn(3L, 5L);
        when(pages.findLatestPublished()).thenReturn(LocalDateTime.ofInstant(start.minusSeconds(90), ZoneId.systemDefault()));
        new WebsiteGauges(registry, tokens, pages, clock);

        assertEquals(3.0, activeTokens());
        assertEquals(90.0, lastPublishAge());
        clock.advance(Duration.ofSeconds(30));
        assertEquals(3.0, activeTokens(), "still the cached value");
        assertEquals(90.0, lastPublishAge(), "the age is cached too, it does not grow between refreshes");
        clock.advance(Duration.ofSeconds(31));
        assertEquals(5.0, activeTokens());
        assertEquals(151.0, lastPublishAge());
        verify(tokens, times(2)).countValid(any());
    }

    @Test
    void aFailingSourceKeepsThePreviousValueAndNothingPublishedIsNaN() {
        when(tokens.countValid(any())).thenReturn(2L)
            .thenThrow(new IllegalStateException("database gone"));
        when(pages.findLatestPublished()).thenReturn(null);
        new WebsiteGauges(registry, tokens, pages, clock);

        assertEquals(2.0, activeTokens());
        assertTrue(Double.isNaN(lastPublishAge()));
        clock.advance(Duration.ofMinutes(2));
        assertEquals(2.0, activeTokens(), "the failed refresh keeps the last value");
    }

    private double activeTokens() {
        return registry.get("vempain.site.tokens.active")
            .gauge()
            .value();
    }

    private double lastPublishAge() {
        return registry.get("vempain.site.last_publish.age.seconds")
            .gauge()
            .value();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
