package com.inplay.decision.clutch;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class ImportanceScorerTest {

    private final ImportanceScorer scorer = new ImportanceScorer();

    private Game game(KboTeam home, KboTeam away) {
        return new Game(new GameId("g1"), LocalDate.of(2026, 5, 12),
                home, away, GameStatus.LIVE, new Score(3, 3));
    }

    private LiveEvent event(int inning, boolean[] runners, OptionalDouble wpaAfter) {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:34:21Z"),
                new GameId("g1"),
                inning, InningHalf.BOTTOM,
                LiveEventType.PITCH,
                1, runners,
                new Score(3, 3),
                Optional.empty(), Optional.empty(), Optional.empty(),
                wpaAfter,
                "src", "id");
    }

    private ClutchVerdict clutch(double p) {
        return ClutchVerdict.ready(p, 0.7);
    }

    @Test
    void zeroWhenVerdictProbabilityNull() {
        var v = ClutchVerdict.modelNotReady(0.7);
        var s = scorer.score(v, game(KboTeam.HH, KboTeam.LG), null,
                event(5, new boolean[3], OptionalDouble.of(0.6)),
                RivalrySettings.noPreference());
        assertThat(s.value()).isZero();
    }

    @Test
    void zeroWhenWpaAfterMissing() {
        var s = scorer.score(clutch(0.8), game(KboTeam.HH, KboTeam.LG), null,
                event(5, new boolean[3], OptionalDouble.empty()),
                RivalrySettings.noPreference());
        assertThat(s.value()).isZero();
    }

    @Test
    void higherWhenMyTeamPlaysRival() {
        var settings = new RivalrySettings(KboTeam.HH, Map.of(KboTeam.LG, 1.3));
        var prev = event(8, new boolean[]{true, true, true}, OptionalDouble.of(0.4));
        var curr = event(8, new boolean[]{true, true, true}, OptionalDouble.of(0.75));
        var s = scorer.score(clutch(0.85), game(KboTeam.HH, KboTeam.LG), prev, curr, settings);
        assertThat(s.value()).isGreaterThan(0.0);
        assertThat(s.rivalryWeight()).isEqualTo(1.3);
    }

    @Test
    void rivalryWeightIs1WhenMyTeamFacesNonRival() {
        var settings = new RivalrySettings(KboTeam.HH, Map.of(KboTeam.LG, 1.3));
        var s = scorer.score(clutch(0.8), game(KboTeam.HH, KboTeam.KIA), null,
                event(8, new boolean[]{true, true, true}, OptionalDouble.of(0.75)),
                RivalrySettings.noPreference());
        assertThat(s.rivalryWeight()).isEqualTo(1.0);
    }

    @Test
    void rivalryWeightFloorWhenMyTeamAbsent() {
        var settings = new RivalrySettings(KboTeam.HH, Map.of(KboTeam.LG, 1.3));
        var s = scorer.score(clutch(0.8), game(KboTeam.KIA, KboTeam.SSG), null,
                event(8, new boolean[3], OptionalDouble.of(0.7)),
                settings);
        assertThat(s.rivalryWeight()).isLessThanOrEqualTo(1.0);
    }

    @Test
    void noMyTeamYieldsNeutralWeight() {
        var s = scorer.score(clutch(0.8), game(KboTeam.HH, KboTeam.LG), null,
                event(8, new boolean[3], OptionalDouble.of(0.75)),
                RivalrySettings.noPreference());
        assertThat(s.rivalryWeight()).isEqualTo(1.0);
    }

    @Test
    void scoreIsClampedTo10() {
        var settings = new RivalrySettings(KboTeam.HH, Map.of(KboTeam.LG, 1.5));
        var prev = event(9, new boolean[]{true, true, true}, OptionalDouble.of(0.0));
        var curr = event(9, new boolean[]{true, true, true}, OptionalDouble.of(1.0));
        var s = scorer.score(clutch(0.99), game(KboTeam.HH, KboTeam.LG), prev, curr, settings);
        assertThat(s.value()).isLessThanOrEqualTo(10.0);
    }

    @Test
    void biggerWpaSwingProducesHigherScore() {
        var settings = RivalrySettings.noPreference();
        var lowPrev = event(7, new boolean[]{false, true, false}, OptionalDouble.of(0.55));
        var lowCurr = event(7, new boolean[]{false, true, false}, OptionalDouble.of(0.58));
        var low = scorer.score(clutch(0.8), game(KboTeam.HH, KboTeam.LG), lowPrev, lowCurr, settings);

        var highPrev = event(7, new boolean[]{true, true, true}, OptionalDouble.of(0.55));
        var highCurr = event(7, new boolean[]{true, true, true}, OptionalDouble.of(0.85));
        var high = scorer.score(clutch(0.8), game(KboTeam.HH, KboTeam.LG), highPrev, highCurr, settings);

        assertThat(high.value()).isGreaterThan(low.value());
    }
}
