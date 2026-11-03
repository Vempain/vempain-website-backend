package fi.poltsi.vempain.website.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import fi.poltsi.vempain.website.repository.WebSiteJwtTokenRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The clean-up removes the tokens expired at the time of the run, reports them as a meter and honours both switches.
 */
class JwtTokenCleanupScheduleUTC {

    private final WebSiteJwtTokenRepository tokens = mock(WebSiteJwtTokenRepository.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final WebsiteMetrics metrics = new WebsiteMetrics(registry);
    private final Instant now = Instant.parse("2026-11-02T10:15:00Z");
    private final Clock clock = Clock.fixed(now, ZoneId.of("UTC"));

    @Test
    void removesTheTokensExpiredNowAndCountsThem() {
        when(tokens.deleteExpired(LocalDateTime.ofInstant(now, ZoneId.of("UTC")))).thenReturn(3);
        var schedule = new JwtTokenCleanupSchedule(tokens, metrics, clock, true, true);

        schedule.removeExpiredTokensScheduled();

        verify(tokens).deleteExpired(LocalDateTime.ofInstant(now, ZoneId.of("UTC")));
        assertEquals(3.0, registry.get("vempain.site.tokens.expired")
            .counter()
            .count());
    }

    @Test
    void aRunWithoutExpiredTokensLeavesTheMeterAlone() {
        when(tokens.deleteExpired(any())).thenReturn(0);

        assertEquals(0, new JwtTokenCleanupSchedule(tokens, metrics, clock).removeExpiredTokens());

        assertEquals(0.0, registry.get("vempain.site.tokens.expired")
            .counter()
            .count());
    }

    @Test
    void theSwitchesStopTheScheduledRun() {
        new JwtTokenCleanupSchedule(tokens, metrics, clock, false, true).removeExpiredTokensScheduled();
        new JwtTokenCleanupSchedule(tokens, metrics, clock, true, false).removeExpiredTokensScheduled();

        verify(tokens, never()).deleteExpired(any());
    }
}
