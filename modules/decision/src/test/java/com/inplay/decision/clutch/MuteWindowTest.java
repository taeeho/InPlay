package com.inplay.decision.clutch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class MuteWindowTest {

    private final ZoneId kst = ZoneId.of("Asia/Seoul");

    @Test
    void noneNeverMutes() {
        assertThat(MuteWindow.none().contains(Instant.parse("2026-05-12T12:34:56Z"))).isFalse();
    }

    @Test
    void sameDayWindowContainsInside() {
        var w = new MuteWindow(LocalTime.of(8, 0), LocalTime.of(10, 0), kst);
        // 09:00 KST = 00:00 UTC
        assertThat(w.contains(Instant.parse("2026-05-12T00:00:00Z"))).isTrue();
    }

    @Test
    void sameDayWindowExcludesOutside() {
        var w = new MuteWindow(LocalTime.of(8, 0), LocalTime.of(10, 0), kst);
        // 11:00 KST = 02:00 UTC
        assertThat(w.contains(Instant.parse("2026-05-12T02:00:00Z"))).isFalse();
    }

    @Test
    void wrapAroundCoversBeforeMidnight() {
        var w = new MuteWindow(LocalTime.of(23, 0), LocalTime.of(8, 0), kst);
        // 23:30 KST = 14:30 UTC
        assertThat(w.contains(Instant.parse("2026-05-12T14:30:00Z"))).isTrue();
    }

    @Test
    void wrapAroundCoversAfterMidnight() {
        var w = new MuteWindow(LocalTime.of(23, 0), LocalTime.of(8, 0), kst);
        // 07:30 KST = 22:30 UTC (전날)
        assertThat(w.contains(Instant.parse("2026-05-11T22:30:00Z"))).isTrue();
    }

    @Test
    void wrapAroundExcludesMidday() {
        var w = new MuteWindow(LocalTime.of(23, 0), LocalTime.of(8, 0), kst);
        assertThat(w.contains(Instant.parse("2026-05-12T03:00:00Z"))).isFalse();
    }
}
