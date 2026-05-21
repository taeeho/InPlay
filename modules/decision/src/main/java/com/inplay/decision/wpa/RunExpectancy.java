package com.inplay.decision.wpa;

// RE24 — expected runs scored from the current state until the end of the half-inning.
// Indexed by [bases.index (0..7)][outs (0..2)].
//
// Source: MLB FanGraphs RE24 table, 2010-2020 league baseline.
//   https://www.fangraphs.com/library/misc/re24/
//   Approximated to 3 decimals from the published averages.
// Reproducibility: run `python/trainer/win_prob/run_expectancy.py` (W3 follow-up)
//   once 1 season of KBO pitch_log is collected → overwrites this table with KBO-calibrated values.
// NOT KBO-native: KBO scoring environment differs (~5–10% lower run scoring in recent years).
// Calibration is gated by the W3 data gate (pitch_log timeseries) — see PLAN.md §10.
public final class RunExpectancy {

    private static final double[][] RE24_MLB_BASELINE = {
            // outs=0  outs=1  outs=2
            {0.481, 0.254, 0.098},   // 000 (empty)
            {0.859, 0.509, 0.224},   // 100 (1st)
            {1.100, 0.664, 0.319},   // 010 (2nd)
            {1.437, 0.884, 0.429},   // 110 (1st+2nd)
            {1.350, 0.950, 0.382},   // 001 (3rd)
            {1.784, 1.130, 0.494},   // 101 (1st+3rd)
            {2.052, 1.467, 0.580},   // 011 (2nd+3rd)
            {2.282, 1.541, 0.752},   // 111 (loaded)
    };

    private RunExpectancy() {
    }

    public static double expectedRuns(Bases bases, int outs) {
        if (outs >= 3) {
            return 0.0;
        }
        if (outs < 0) {
            throw new IllegalArgumentException("outs must be >= 0, got " + outs);
        }
        return RE24_MLB_BASELINE[bases.index()][outs];
    }
}
