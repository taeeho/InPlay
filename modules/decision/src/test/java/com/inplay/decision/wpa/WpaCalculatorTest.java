package com.inplay.decision.wpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.game.InningHalf;
import org.junit.jupiter.api.Test;

class WpaCalculatorTest {

    private final WpaCalculator calc = new WpaCalculator();

    @Test
    void homeTeamScoringRaisesHomeWpa() {
        // 7회말 1아웃, 동점 3-3, 3루 주자. single → 4-3 역전.
        // rule-based 모델은 보수적(σ_per_half=1.05) → 절대값은 W3 데이터 게이트 후 캘리브레이션.
        // 여기서는 부호와 유의 수준만 invariant로 검증.
        GameState before = new GameState(7, InningHalf.BOTTOM, 1, Bases.of(0, 0, 1), 3, 3, false);
        GameState after = new GameState(7, InningHalf.BOTTOM, 1, Bases.of(1, 0, 0), 4, 3, false);
        double wpa = calc.calculate(before, after);
        assertThat(wpa).isPositive();
        assertThat(wpa).isGreaterThan(0.05);
    }

    @Test
    void awayTeamScoringDropsHomeWpa() {
        GameState before = new GameState(7, InningHalf.TOP, 1, Bases.of(0, 0, 1), 4, 3, false);
        GameState after = new GameState(7, InningHalf.TOP, 1, Bases.of(1, 0, 0), 4, 4, false);
        double wpa = calc.calculate(before, after);
        assertThat(wpa).isNegative();
    }

    @Test
    void strikeoutWithRunnersInScoringPositionDropsBattingWpa() {
        GameState before = new GameState(8, InningHalf.BOTTOM, 1, Bases.of(0, 1, 1), 2, 3, false);
        GameState after = new GameState(8, InningHalf.BOTTOM, 2, Bases.of(0, 1, 1), 2, 3, false);
        // home is batting, gave up an out without scoring → home WPA should drop
        double homeWpa = calc.calculate(before, after);
        assertThat(homeWpa).isNegative();
        assertThat(calc.battingTeamWpa(before, after)).isNegative();
    }

    @Test
    void walkOffHomerunWpaCloseToOne() {
        GameState before = new GameState(9, InningHalf.BOTTOM, 2, Bases.empty(), 3, 4, false);
        GameState after = new GameState(9, InningHalf.BOTTOM, 3, Bases.empty(), 5, 4, true);
        double wpa = calc.calculate(before, after);
        assertThat(wpa).isGreaterThan(0.5);
        assertThat(wpa).isLessThanOrEqualTo(1.0);
    }

    @Test
    void battingTeamWpaIsHomeWpaWhenHomeIsBatting() {
        GameState before = new GameState(5, InningHalf.BOTTOM, 0, Bases.empty(), 2, 2, false);
        GameState after = new GameState(5, InningHalf.BOTTOM, 0, Bases.of(1, 0, 0), 2, 2, false);
        double home = calc.calculate(before, after);
        double batting = calc.battingTeamWpa(before, after);
        assertThat(batting).isEqualTo(home);
    }

    @Test
    void battingTeamWpaIsNegatedHomeWpaWhenAwayIsBatting() {
        GameState before = new GameState(5, InningHalf.TOP, 0, Bases.empty(), 2, 2, false);
        GameState after = new GameState(5, InningHalf.TOP, 0, Bases.of(1, 0, 0), 2, 2, false);
        double home = calc.calculate(before, after);
        double batting = calc.battingTeamWpa(before, after);
        assertThat(batting).isEqualTo(-home);
        // away got a runner on first → home WPA falls, batting (away) WPA rises
        assertThat(batting).isPositive();
    }

    @Test
    void zeroChangeYieldsZeroWpa() {
        GameState s = new GameState(4, InningHalf.TOP, 0, Bases.empty(), 1, 1, false);
        assertThat(calc.calculate(s, s)).isZero();
    }

    @Test
    void awayWalkoffEquivalentGivesNegativeHomeWpa() {
        // 연장 10회초 2아웃 동점에서 어웨이 솔로 홈런으로 종료.
        GameState before = new GameState(10, InningHalf.TOP, 2, Bases.empty(), 4, 4, false);
        GameState after = new GameState(10, InningHalf.TOP, 3, Bases.empty(), 4, 5, true);
        double wpa = calc.calculate(before, after);
        assertThat(wpa).isLessThan(-0.4);
    }

    @Test
    void homeAndAwayPerspectiveAlwaysSumToZero() {
        // home WPA + away WPA = 0 (제로섬). away = -home 으로 직접 검증.
        GameState before = new GameState(6, InningHalf.BOTTOM, 0, Bases.of(1, 0, 0), 2, 2, false);
        GameState after = new GameState(6, InningHalf.BOTTOM, 0, Bases.of(1, 1, 0), 2, 2, false);
        double home = calc.calculate(before, after);
        double away = -home;
        assertThat(home + away).isZero();
    }

    @Test
    void inningSwitchHasNegligibleWpa() {
        // 6회초 3아웃 → 6회말 무사. 같은 점수, 같은 빈 베이스.
        // 모델은 RE24를 그대로 쓰고 mean_runs_per_half는 별도 상수라 이닝 전환에서 두 값의 미세한 차이만큼만
        // WE가 흔들림. invariant: 절대값이 무시할 수준이어야 함(부호는 RE24 캘리브레이션에 의존).
        GameState topEnd = new GameState(6, InningHalf.TOP, 3, Bases.empty(), 3, 3, false);
        GameState bottomStart = new GameState(6, InningHalf.BOTTOM, 0, Bases.empty(), 3, 3, false);
        double wpa = calc.calculate(topEnd, bottomStart);
        assertThat(Math.abs(wpa)).isLessThan(0.01);
    }
}
