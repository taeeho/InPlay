package com.inplay.api.clutch;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inplay.clutch")
public record ClutchProperties(
        boolean enabled,
        double importanceThreshold,
        Duration cooldown,
        Duration dedupeTtl,
        double detectorThreshold) {
    public ClutchProperties {
        if (cooldown == null) cooldown = Duration.ofMinutes(5);
        if (dedupeTtl == null) dedupeTtl = Duration.ofMinutes(1);
    }
}
