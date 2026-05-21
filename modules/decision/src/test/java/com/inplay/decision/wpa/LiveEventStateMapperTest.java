package com.inplay.decision.wpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class LiveEventStateMapperTest {

    private LiveEvent event(int inning, InningHalf half, int outs, boolean[] runners,
                            int homeRuns, int awayRuns) {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:34:21Z"),
                new GameId("g1"),
                inning, half,
                LiveEventType.PITCH,
                outs, runners,
                new Score(homeRuns, awayRuns),
                Optional.empty(), Optional.empty(), Optional.empty(),
                OptionalDouble.empty(),
                "src", "src:1");
    }

    @Test
    void mapsCoreFieldsDirectly() {
        var e = event(5, InningHalf.BOTTOM, 2, new boolean[]{true, true, true}, 3, 4);
        var s = LiveEventStateMapper.toGameState(e);
        assertThat(s.inning()).isEqualTo(5);
        assertThat(s.half()).isEqualTo(InningHalf.BOTTOM);
        assertThat(s.outs()).isEqualTo(2);
        assertThat(s.homeRuns()).isEqualTo(3);
        assertThat(s.awayRuns()).isEqualTo(4);
        assertThat(s.gameOver()).isFalse();
    }

    @Test
    void runnersArrayMapsToBasesByPosition() {
        var loaded = event(1, InningHalf.TOP, 0, new boolean[]{true, true, true}, 0, 0);
        assertThat(LiveEventStateMapper.toGameState(loaded).bases().loaded()).isTrue();

        var second = event(1, InningHalf.TOP, 0, new boolean[]{false, true, false}, 0, 0);
        var b = LiveEventStateMapper.toGameState(second).bases();
        assertThat(b.first()).isFalse();
        assertThat(b.second()).isTrue();
        assertThat(b.third()).isFalse();
    }

    @Test
    void emptyBasesMappedCorrectly() {
        var e = event(3, InningHalf.TOP, 1, new boolean[3], 1, 2);
        assertThat(LiveEventStateMapper.toGameState(e).bases().runnersOn()).isZero();
    }
}
