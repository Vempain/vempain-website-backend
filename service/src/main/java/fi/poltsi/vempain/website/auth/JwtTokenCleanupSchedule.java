package fi.poltsi.vempain.website.auth;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.website.metrics.WebsiteMetrics;
import fi.poltsi.vempain.website.repository.WebSiteJwtTokenRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Removes the expired visitor session tokens from {@code web_site_jwt_token}. Every login stores a row and a logout removes its own row, but a session that
 * simply ends by expiry leaves its row behind, so without this job the table grows with every visit. Expired rows are never read again ({@code findValidToken}
 * and {@code countValid} only look at unexpired ones), so removing them changes nothing for the visitors.
 */
@Slf4j
@Service
public class JwtTokenCleanupSchedule {

    private final WebSiteJwtTokenRepository tokens;
    private final WebsiteMetrics metrics;
    private final Clock clock;

    @Value("${vempain.jwt-cleanup.enabled:true}")
    private boolean cleanupEnabled = true;

    @Value("${vempain.scheduling.enabled:true}")
    private boolean schedulingEnabled = true;

    @Autowired
    public JwtTokenCleanupSchedule(WebSiteJwtTokenRepository tokens, WebsiteMetrics metrics) {
        this(tokens, metrics, Clock.systemDefaultZone());
    }

    JwtTokenCleanupSchedule(WebSiteJwtTokenRepository tokens, WebsiteMetrics metrics, Clock clock) {
        this.tokens = tokens;
        this.metrics = metrics;
        this.clock = clock;
    }

    JwtTokenCleanupSchedule(WebSiteJwtTokenRepository tokens, WebsiteMetrics metrics, Clock clock, boolean cleanupEnabled, boolean schedulingEnabled) {
        this(tokens, metrics, clock);
        this.cleanupEnabled = cleanupEnabled;
        this.schedulingEnabled = schedulingEnabled;
    }

    /** Hourly by default; {@code vempain.jwt-cleanup.cron} changes the rhythm, {@code vempain.jwt-cleanup.enabled} switches the job off */
    @Scheduled(cron = "${vempain.jwt-cleanup.cron:0 15 * * * *}")
    public void removeExpiredTokensScheduled() {
        if (!schedulingEnabled
            || !cleanupEnabled) {
            return;
        }
        removeExpiredTokens();
    }

    /**
     * Deletes every token whose expiry lies in the past.
     *
     * @return the number of rows removed
     */
    @Transactional
    public int removeExpiredTokens() {
        var now = LocalDateTime.now(clock);
        var removed = tokens.deleteExpired(now);
        if (removed > 0) {
            log.info("Removed {} expired visitor session token(s)", removed);
            metrics.tokensExpired(removed);
        } else {
            log.debug("No expired visitor session tokens to remove");
        }
        return removed;
    }
}
