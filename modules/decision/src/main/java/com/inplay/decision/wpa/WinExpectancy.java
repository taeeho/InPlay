package com.inplay.decision.wpa;

import com.inplay.core.domain.game.InningHalf;

// Rule-based win expectancy from the HOME team perspective.
//
// ⚠ MVP rule-based approximation, NOT a production WE model.
// Calibration against KBO data is gated by the W3 data gate (PLAN.md §10);
// until then, absolute WPA magnitudes are conservative (under-confident in 1-run leads late).
// Sign and ordinality (more runners → higher batting WE, late lead → higher leader WE) are
// invariant by construction and tested directly.
//
// Approach:
//   1. gameOver → 1.0 / 0.5 / 0.0 by final margin.
//   2. otherwise, estimate expected runs remaining for each team:
//      - batting team: RE24(bases, outs) + (own half-innings remaining after this one) * MEAN_RUNS_PER_HALF_MLB
//      - fielding team: (own half-innings remaining) * MEAN_RUNS_PER_HALF_MLB
//      Variance of final margin grows linearly with halves remaining (independent Normal approx).
//   3. WE(home) = 1 - Φ(0 | meanHomeMargin, sigma) — continuous Normal CDF via Abramowitz erf.
//
// Endgame contract: the caller is responsible for setting gameOver=true at the right moment
// (regulation end after bottom of 9th if home leads at top end / regulation end / both halves complete,
// walk-off when home scores go-ahead run in bottom of 9th or later, away win when away leads after
// bottom of 9th). This module is pure: it does not implement KBO rulebook itself.
public final class WinExpectancy {

    private static final double MEAN_RUNS_PER_HALF_MLB = 0.5;     // ≈ 9 runs / 18 halves (MLB 2010-2020)
    private static final double STDDEV_PER_HALF_MLB = 1.05;       // empirical run StDev, MLB baseline
    private static final int REGULATION_INNINGS = 9;
    private static final int MAX_EXTRA_INNINGS_FOR_ESTIMATE = 3;

    private WinExpectancy() {
    }

    public static double homeWinProb(GameState s) {
        if (s.gameOver()) {
            int m = s.margin();
            if (m > 0) return 1.0;
            if (m < 0) return 0.0;
            return 0.5;
        }

        int halvesRemainingThisAndAfter = halvesRemaining(s.inning(), s.half(), s.outs() >= 3);
        int currentHalfStillLive = (s.outs() < 3) ? 1 : 0;
        int halvesAfterCurrent = halvesRemainingThisAndAfter - currentHalfStillLive;

        boolean battingIsHome = s.battingIsHome();
        double remainingThisHalf = (s.outs() < 3)
                ? RunExpectancy.expectedRuns(s.bases(), s.outs())
                : 0.0;

        int battingHalvesAfter;
        int fieldingHalvesAfter;
        if (battingIsHome) {
            fieldingHalvesAfter = (halvesAfterCurrent + 1) / 2;
            battingHalvesAfter = halvesAfterCurrent / 2;
        } else {
            battingHalvesAfter = halvesAfterCurrent / 2;
            fieldingHalvesAfter = (halvesAfterCurrent + 1) / 2;
        }

        double battingExtra = remainingThisHalf + battingHalvesAfter * MEAN_RUNS_PER_HALF_MLB;
        double fieldingExtra = fieldingHalvesAfter * MEAN_RUNS_PER_HALF_MLB;

        double battingFinal = (battingIsHome ? s.homeRuns() : s.awayRuns()) + battingExtra;
        double fieldingFinal = (battingIsHome ? s.awayRuns() : s.homeRuns()) + fieldingExtra;

        double meanHomeMargin = battingIsHome
                ? (battingFinal - fieldingFinal)
                : (fieldingFinal - battingFinal);

        int totalHalves = battingHalvesAfter + fieldingHalvesAfter + currentHalfStillLive;
        double variance = Math.max(totalHalves, 1) * STDDEV_PER_HALF_MLB * STDDEV_PER_HALF_MLB;
        double sigma = Math.sqrt(variance);

        return 1.0 - normCdf(0.0, meanHomeMargin, sigma);
    }

    private static int halvesRemaining(int inning, InningHalf half, boolean halfFinished) {
        int innings = Math.min(Math.max(inning, 1), REGULATION_INNINGS + MAX_EXTRA_INNINGS_FOR_ESTIMATE);
        int halvesPlayed = (innings - 1) * 2 + (half == InningHalf.BOTTOM ? 1 : 0) + (halfFinished ? 1 : 0);
        int totalHalves = REGULATION_INNINGS * 2;
        if (innings > REGULATION_INNINGS) {
            totalHalves = innings * 2;
        }
        return Math.max(totalHalves - halvesPlayed, 0);
    }

    private static double normCdf(double x, double mu, double sigma) {
        if (sigma < 1e-9) {
            if (x < mu) return 1.0;
            if (x > mu) return 0.0;
            return 0.5;
        }
        double z = (x - mu) / sigma;
        return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
    }

    // Abramowitz & Stegun 7.1.26 — max error ~1.5e-7
    private static double erf(double x) {
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;
        int sign = x < 0 ? -1 : 1;
        double ax = Math.abs(x);
        double t = 1.0 / (1.0 + p * ax);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-ax * ax);
        return sign * y;
    }
}
