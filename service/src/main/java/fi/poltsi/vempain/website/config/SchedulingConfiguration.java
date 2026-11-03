package fi.poltsi.vempain.website.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the {@code @Scheduled} jobs of the service (the expired session token clean-up); every job also honours the {@code vempain.scheduling.enabled}
 * switch, so one replica can run them while the others answer requests only.
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
