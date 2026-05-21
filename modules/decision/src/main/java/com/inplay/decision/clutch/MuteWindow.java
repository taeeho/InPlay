package com.inplay.decision.clutch;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 사용자 알림 침묵 시간대 (PLAN.md §6 step4).
 *
 * <p>start≤end → 같은 날 구간 (e.g. 23:00→08:00 X — 다음 케이스).
 * start>end  → 자정 넘는 구간 (e.g. 23:00→08:00). 22:00·07:00 모두 muted.
 */
public record MuteWindow(LocalTime start, LocalTime end, ZoneId zoneId) {
    public MuteWindow {
        Objects.requireNonNull(start, "start required");
        Objects.requireNonNull(end, "end required");
        Objects.requireNonNull(zoneId, "zoneId required");
    }

    public static MuteWindow none() {
        return new MuteWindow(LocalTime.MIN, LocalTime.MIN, ZoneId.of("Asia/Seoul"));
    }

    public boolean contains(Instant at) {
        if (start.equals(end)) {
            return false;
        }
        LocalTime t = at.atZone(zoneId).toLocalTime();
        if (start.isBefore(end)) {
            return !t.isBefore(start) && t.isBefore(end);
        }
        // wrap-around
        return !t.isBefore(start) || t.isBefore(end);
    }
}
