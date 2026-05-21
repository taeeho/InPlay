package com.inplay.decision.clutch;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class ClutchFeatureBuilderTest {

    private static final Offset<Double> EPS = Offset.offset(1e-9);
    private final ClutchFeatureBuilder builder = new ClutchFeatureBuilder();

    private LiveEvent event(int inning, int outs, boolean[] runners,
                            int homeRuns, int awayRuns, OptionalDouble wpaAfter) {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:34:21Z"),
                new GameId("g1"),
                inning, InningHalf.BOTTOM,
                LiveEventType.PITCH,
                outs, runners,
                new Score(homeRuns, awayRuns),
                Optional.empty(), Optional.empty(), Optional.empty(),
                wpaAfter,
                "src", "id");
    }

    @Test
    void returnsEmptyWhenCurrentHasNoWpa() {
        var curr = event(5, 1, new boolean[3], 3, 3, OptionalDouble.empty());
        assertThat(builder.build(null, curr)).isEmpty();
    }

    @Test
    void firstEventOfGameUsesHalfAsBaseline() {
        // prev null → we_before = 0.5. we_after=0.62 → wpa_change=0.12
        var curr = event(1, 0, new boolean[3], 0, 0, OptionalDouble.of(0.62));
        var f = builder.build(null, curr).orElseThrow();
        assertThat(f.wpaChangeAbs()).isCloseTo(0.12, EPS);
        // we_balance = 1 - 2*|0.62 - 0.5| = 0.76
        assertThat(f.weBalance()).isCloseTo(0.76, EPS);
    }

    @Test
    void usesPrevWpaWhenAvailable() {
        var prev = event(7, 0, new boolean[]{false, true, false}, 3, 3, OptionalDouble.of(0.55));
        var curr = event(7, 1, new boolean[]{false, true, false}, 3, 3, OptionalDouble.of(0.40));
        var f = builder.build(prev, curr).orElseThrow();
        assertThat(f.wpaChangeAbs()).isCloseTo(0.15, EPS);
    }

    @Test
    void inningProgressCapsAtOne() {
        var curr = event(12, 0, new boolean[3], 3, 3, OptionalDouble.of(0.5));
        var f = builder.build(null, curr).orElseThrow();
        assertThat(f.inningProgress()).isCloseTo(1.0, EPS);
    }

    @Test
    void scoreMarginNormCapsAtOne() {
        var curr = event(5, 0, new boolean[3], 10, 0, OptionalDouble.of(0.95));
        var f = builder.build(null, curr).orElseThrow();
        assertThat(f.scoreMarginNorm()).isCloseTo(1.0, EPS);
    }

    @Test
    void runnersOnNormCountsAllThreeBases() {
        var curr = event(7, 1, new boolean[]{true, true, true}, 3, 3, OptionalDouble.of(0.5));
        var f = builder.build(null, curr).orElseThrow();
        assertThat(f.runnersOnNorm()).isCloseTo(1.0, EPS);
    }

    @Test
    void leverageProxyCombinesSwingInningRunners() {
        var prev = event(8, 1, new boolean[]{true, true, true}, 3, 3, OptionalDouble.of(0.50));
        var curr = event(8, 1, new boolean[]{true, true, true}, 3, 3, OptionalDouble.of(0.80));
        var late = builder.build(prev, curr).orElseThrow();

        var earlyPrev = event(2, 1, new boolean[3], 0, 0, OptionalDouble.of(0.50));
        var earlyCurr = event(2, 1, new boolean[3], 0, 0, OptionalDouble.of(0.55));
        var early = builder.build(earlyPrev, earlyCurr).orElseThrow();

        assertThat(late.leverageProxy()).isGreaterThan(early.leverageProxy());
    }

    @Test
    void leverageProxyIsBoundedAt1_5() {
        // forcing max: prev=0, curr=1.0 → wpaChange=1.0; inning=9; loaded
        var prev = event(9, 0, new boolean[]{true, true, true}, 0, 0, OptionalDouble.of(0.0));
        var curr = event(9, 0, new boolean[]{true, true, true}, 0, 0, OptionalDouble.of(1.0));
        var f = builder.build(prev, curr).orElseThrow();
        assertThat(f.leverageProxy()).isLessThanOrEqualTo(1.5);
    }
}
