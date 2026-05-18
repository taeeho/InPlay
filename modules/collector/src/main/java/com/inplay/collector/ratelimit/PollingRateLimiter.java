package com.inplay.collector.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PollingRateLimiter {

    private final Clock clock;
    private final ConcurrentHashMap<String, Instant> lastAcquireByKey = new ConcurrentHashMap<>();

    public PollingRateLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock required");
    }

    public boolean tryAcquire(String key, PollingMode mode) {
        Objects.requireNonNull(key, "key required");
        Objects.requireNonNull(mode, "mode required");
        Instant now = clock.instant();
        Duration min = mode.minInterval();
        AtomicBoolean acquired = new AtomicBoolean(false);
        lastAcquireByKey.compute(key, (k, last) -> {
            if (last == null || Duration.between(last, now).compareTo(min) >= 0) {
                acquired.set(true);
                return now;
            }
            return last;
        });
        return acquired.get();
    }
}
