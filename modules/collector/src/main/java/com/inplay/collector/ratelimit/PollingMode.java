package com.inplay.collector.ratelimit;

import java.time.Duration;

public enum PollingMode {
    NORMAL(Duration.ofSeconds(60)),
    LIVE(Duration.ofSeconds(30));

    private final Duration minInterval;

    PollingMode(Duration minInterval) {
        this.minInterval = minInterval;
    }

    public Duration minInterval() {
        return minInterval;
    }
}
