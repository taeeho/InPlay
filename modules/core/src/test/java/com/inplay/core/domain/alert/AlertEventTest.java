package com.inplay.core.domain.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.id.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AlertEventTest {

    private static final UserId USER = new UserId("u_taeeho");
    private static final GameId GAME = new GameId("20260512HHLG");
    private static final Instant TS = Instant.parse("2026-05-12T10:34:21Z");

    @Test
    void validAlertAccepted() {
        AlertEvent alert =
                new AlertEvent(USER, GAME, TS, AlertCategory.CRISIS_MOMENT, 7.5, "5회말 만루 위기");
        assertThat(alert.importance()).isEqualTo(7.5);
        assertThat(alert.category()).isEqualTo(AlertCategory.CRISIS_MOMENT);
    }

    @Test
    void importanceAboveTenRejected() {
        assertThatThrownBy(
                () -> new AlertEvent(USER, GAME, TS, AlertCategory.CRISIS_MOMENT, 10.0001, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("importance");
    }

    @Test
    void importanceBelowZeroRejected() {
        assertThatThrownBy(
                () -> new AlertEvent(USER, GAME, TS, AlertCategory.CRISIS_MOMENT, -0.1, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nanImportanceRejected() {
        assertThatThrownBy(
                () -> new AlertEvent(USER, GAME, TS, AlertCategory.CRISIS_MOMENT, Double.NaN, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullCategoryRejected() {
        assertThatThrownBy(() -> new AlertEvent(USER, GAME, TS, null, 1.0, "x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blankMessageRejected() {
        assertThatThrownBy(() -> new AlertEvent(USER, GAME, TS, AlertCategory.GAME_END, 1.0, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
