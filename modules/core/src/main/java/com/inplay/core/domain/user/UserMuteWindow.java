package com.inplay.core.domain.user;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 사용자 알림 침묵 시간대 (PLAN.md §3 user.mute_window).
 *
 * <p>start ≤ end → 같은 날 구간 (예: 08:00→10:00).
 * start  > end  → 자정 넘는 구간 (예: 23:00→07:00).
 */
public record UserMuteWindow(LocalTime start, LocalTime end, ZoneId zoneId) {
    public UserMuteWindow {
        Objects.requireNonNull(start, "start required");
        Objects.requireNonNull(end, "end required");
        Objects.requireNonNull(zoneId, "zoneId required");
    }

    public static UserMuteWindow disabled() {
        return new UserMuteWindow(LocalTime.MIN, LocalTime.MIN, ZoneId.of("Asia/Seoul"));
    }

    public boolean isDisabled() {
        return start.equals(end);
    }
}
