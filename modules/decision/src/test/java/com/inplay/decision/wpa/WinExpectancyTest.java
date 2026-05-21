package com.inplay.decision.wpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.game.InningHalf;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class WinExpectancyTest {

    private static final Offset<Double> EPS = Offset.offset(0.03);

    @Test
    void freshGameTopOfFirstNearFiftyPercent() {
        GameState s = new GameState(1, InningHalf.TOP, 0, Bases.empty(), 0, 0, false);
        double we = WinExpectancy.homeWinProb(s);
        // Home has slight advantage (one extra half) but rule-based model is close to 0.5
        assertThat(we).isBetween(0.45, 0.55);
    }

    @Test
    void gameOverHomeLeadsReturnsOne() {
        GameState s = new GameState(9, InningHalf.BOTTOM, 3, Bases.empty(), 5, 3, true);
        assertThat(WinExpectancy.homeWinProb(s)).isEqualTo(1.0);
    }

    @Test
    void gameOverAwayLeadsReturnsZero() {
        GameState s = new GameState(9, InningHalf.BOTTOM, 3, Bases.empty(), 2, 7, true);
        assertThat(WinExpectancy.homeWinProb(s)).isEqualTo(0.0);
    }

    @Test
    void gameOverTieReturnsHalf() {
        GameState s = new GameState(12, InningHalf.BOTTOM, 3, Bases.empty(), 4, 4, true);
        assertThat(WinExpectancy.homeWinProb(s)).isEqualTo(0.5);
    }

    @Test
    void homeLeadingLateInningGreaterThanFifty() {
        // Bottom of the 8th, home leads by 2 — should be well above 0.5
        GameState s = new GameState(8, InningHalf.BOTTOM, 0, Bases.empty(), 5, 3, false);
        assertThat(WinExpectancy.homeWinProb(s)).isGreaterThan(0.75);
    }

    @Test
    void awayLeadingLateInningLessThanFifty() {
        GameState s = new GameState(8, InningHalf.TOP, 0, Bases.empty(), 3, 5, false);
        assertThat(WinExpectancy.homeWinProb(s)).isLessThan(0.30);
    }

    @Test
    void runnersInScoringPositionRaiseBattingTeamWe() {
        GameState empty = new GameState(7, InningHalf.BOTTOM, 0, Bases.empty(), 3, 3, false);
        GameState loaded = new GameState(7, InningHalf.BOTTOM, 0, Bases.of(1, 1, 1), 3, 3, false);
        // home is batting → loaded should raise home WE
        assertThat(WinExpectancy.homeWinProb(loaded)).isGreaterThan(WinExpectancy.homeWinProb(empty));
    }

    @Test
    void bigLeadIsNearOne() {
        GameState s = new GameState(5, InningHalf.TOP, 0, Bases.empty(), 12, 0, false);
        assertThat(WinExpectancy.homeWinProb(s)).isCloseTo(1.0, EPS);
    }

    @Test
    void bigDeficitIsNearZero() {
        GameState s = new GameState(5, InningHalf.TOP, 0, Bases.empty(), 0, 12, false);
        assertThat(WinExpectancy.homeWinProb(s)).isCloseTo(0.0, EPS);
    }
}
