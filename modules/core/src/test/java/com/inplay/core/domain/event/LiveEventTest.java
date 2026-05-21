package com.inplay.core.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.id.PlayerId;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class LiveEventTest {

    private LiveEvent sample() {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:34:21Z"),
                new GameId("20260512HHLG"),
                5,
                InningHalf.BOTTOM,
                LiveEventType.PITCH,
                2,
                new boolean[]{true, true, true},
                new Score(3, 4),
                Optional.of(new PlayerId("p_batter")),
                Optional.of(new PlayerId("p_pitcher")),
                Optional.of(new PitchInfo("FF", 147, "ball")),
                OptionalDouble.empty(),
                "naver_live",
                "naver:abc123");
    }

    @Test
    void buildsHappyPath() {
        var e = sample();
        assertThat(e.gameId().value()).isEqualTo("20260512HHLG");
        assertThat(e.runnerOn(1)).isTrue();
        assertThat(e.runnerOn(2)).isTrue();
        assertThat(e.runnerOn(3)).isTrue();
    }

    @Test
    void runnersArrayIsDefensivelyCopied() {
        boolean[] r = {true, false, true};
        var e = new LiveEvent(
                Instant.now(), new GameId("g"), 1, InningHalf.TOP,
                LiveEventType.PITCH, 0, r, Score.zero(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                OptionalDouble.empty(), "src", "src:1");
        r[0] = false;
        assertThat(e.runnerOn(1)).isTrue();
        var got = e.runners();
        got[1] = true;
        assertThat(e.runnerOn(2)).isFalse();
    }

    @Test
    void rejectsBlankSourceEventId() {
        assertThatThrownBy(() -> new LiveEvent(
                Instant.now(), new GameId("g"), 1, InningHalf.TOP,
                LiveEventType.PITCH, 0, new boolean[3], Score.zero(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                OptionalDouble.empty(), "src", "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWrongRunnerArrayLength() {
        assertThatThrownBy(() -> new LiveEvent(
                Instant.now(), new GameId("g"), 1, InningHalf.TOP,
                LiveEventType.PITCH, 0, new boolean[]{true, false}, Score.zero(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                OptionalDouble.empty(), "src", "id"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withWpaAfterReturnsCopyWithStampedValue() {
        var base = sample();
        assertThat(base.wpaAfter()).isEmpty();
        var stamped = base.withWpaAfter(0.412);
        assertThat(stamped.wpaAfter()).hasValue(0.412);
        assertThat(base.wpaAfter()).isEmpty();
        assertThat(stamped.sourceEventId()).isEqualTo(base.sourceEventId());
    }

    @Test
    void rejectsOutOfRangeOuts() {
        assertThatThrownBy(() -> new LiveEvent(
                Instant.now(), new GameId("g"), 1, InningHalf.TOP,
                LiveEventType.PITCH, 4, new boolean[3], Score.zero(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                OptionalDouble.empty(), "src", "id"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
