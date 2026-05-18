package com.inplay.collector.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PollingRateLimiterTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-05-12T10:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public Instant instant() { return now.get(); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
    };

    @Test
    void firstAcquireSucceeds() {
        var limiter = new PollingRateLimiter(clock);
        assertThat(limiter.tryAcquire("kbo", PollingMode.NORMAL)).isTrue();
    }

    @Test
    void secondAcquireBeforeMinIntervalBlocked() {
        var limiter = new PollingRateLimiter(clock);
        assertThat(limiter.tryAcquire("kbo", PollingMode.NORMAL)).isTrue();
        now.set(now.get().plus(Duration.ofSeconds(30)));
        assertThat(limiter.tryAcquire("kbo", PollingMode.NORMAL)).isFalse();
    }

    @Test
    void acquireAfterMinIntervalAllowed() {
        var limiter = new PollingRateLimiter(clock);
        assertThat(limiter.tryAcquire("kbo", PollingMode.NORMAL)).isTrue();
        now.set(now.get().plus(Duration.ofSeconds(60)));
        assertThat(limiter.tryAcquire("kbo", PollingMode.NORMAL)).isTrue();
    }

    @Test
    void liveModeAllowsThirtySecondInterval() {
        var limiter = new PollingRateLimiter(clock);
        assertThat(limiter.tryAcquire("naver", PollingMode.LIVE)).isTrue();
        now.set(now.get().plus(Duration.ofSeconds(29)));
        assertThat(limiter.tryAcquire("naver", PollingMode.LIVE)).isFalse();
        now.set(now.get().plus(Duration.ofSeconds(1)));
        assertThat(limiter.tryAcquire("naver", PollingMode.LIVE)).isTrue();
    }

    @Test
    void differentKeysIndependent() {
        var limiter = new PollingRateLimiter(clock);
        assertThat(limiter.tryAcquire("kbo", PollingMode.NORMAL)).isTrue();
        assertThat(limiter.tryAcquire("naver", PollingMode.LIVE)).isTrue();
    }
}
