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

class WpaAnnotatorTest {

    private final WpaAnnotator annotator = new WpaAnnotator();

    private LiveEvent event(int inning, InningHalf half, int outs, boolean[] runners,
                            int homeRuns, int awayRuns, OptionalDouble wpaAfter) {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:34:21Z"),
                new GameId("g1"),
                inning, half,
                LiveEventType.PITCH,
                outs, runners,
                new Score(homeRuns, awayRuns),
                Optional.empty(), Optional.empty(), Optional.empty(),
                wpaAfter,
                "src", "src:1");
    }

    @Test
    void stampsHomeWinProbabilityOnFreshEvent() {
        var e = event(1, InningHalf.TOP, 0, new boolean[3], 0, 0, OptionalDouble.empty());
        var stamped = annotator.annotate(e);
        assertThat(stamped.wpaAfter()).isPresent();
        double we = stamped.wpaAfter().getAsDouble();
        assertThat(we).isBetween(0.45, 0.55);
    }

    @Test
    void homeLeadingLateGameRaisesWpaAfter() {
        var e = event(8, InningHalf.BOTTOM, 0, new boolean[3], 5, 3, OptionalDouble.empty());
        double we = annotator.annotate(e).wpaAfter().getAsDouble();
        assertThat(we).isGreaterThan(0.75);
    }

    @Test
    void awayLeadingLateGameLowersWpaAfter() {
        var e = event(8, InningHalf.TOP, 0, new boolean[3], 3, 5, OptionalDouble.empty());
        double we = annotator.annotate(e).wpaAfter().getAsDouble();
        assertThat(we).isLessThan(0.30);
    }

    @Test
    void reAnnotatingOverwritesPreviousWpa() {
        var e = event(5, InningHalf.BOTTOM, 0, new boolean[]{true, true, true}, 3, 3,
                OptionalDouble.of(0.99));
        var stamped = annotator.annotate(e);
        assertThat(stamped.wpaAfter()).isPresent();
        assertThat(stamped.wpaAfter().getAsDouble()).isNotEqualTo(0.99);
        assertThat(stamped.wpaAfter().getAsDouble()).isBetween(0.0, 1.0);
    }

    @Test
    void annotateDoesNotMutateInput() {
        var e = event(1, InningHalf.TOP, 0, new boolean[3], 0, 0, OptionalDouble.empty());
        annotator.annotate(e);
        assertThat(e.wpaAfter()).isEmpty();
    }

    @Test
    void deltaWeAcrossPlaysIsRecoverableBySubtraction() {
        // 6회말 0아웃, 2-2, 빈 베이스 → 1루타로 1루 주자
        var before = event(6, InningHalf.BOTTOM, 0, new boolean[3], 2, 2, OptionalDouble.empty());
        var after  = event(6, InningHalf.BOTTOM, 0, new boolean[]{true, false, false}, 2, 2, OptionalDouble.empty());
        double weBefore = annotator.annotate(before).wpaAfter().getAsDouble();
        double weAfter  = annotator.annotate(after).wpaAfter().getAsDouble();
        // home (batting) gained a runner → WE_home rises
        assertThat(weAfter).isGreaterThan(weBefore);
    }
}
