package com.inplay.decision.wpa;

import com.inplay.core.domain.game.InningHalf;

import java.util.Objects;

// GameState — immutable snapshot for WPA calculation.
//
// gameOver contract:
//   - true iff the game has ended per KBO rule (regulation end after bottom 9 / walk-off / extras decided).
//   - Caller is responsible for this flag. WinExpectancy treats gameOver=true as terminal (0/0.5/1.0).
//   - DO NOT set gameOver=true mid-inning unless walk-off applies (home goes ahead in bottom of 9th+).
//
// Perspective convention:
//   - homeRuns / awayRuns are absolute (NOT batting/fielding).
//   - half=TOP → away is batting; half=BOTTOM → home is batting.
//   - WinExpectancy returns home-team P(win). Negate for away.
public record GameState(
        int inning,
        InningHalf half,
        int outs,
        Bases bases,
        int homeRuns,
        int awayRuns,
        boolean gameOver
) {
    public GameState {
        if (inning < 1) {
            throw new IllegalArgumentException("inning must be >= 1, got " + inning);
        }
        if (outs < 0 || outs > 3) {
            throw new IllegalArgumentException("outs must be in 0..3, got " + outs);
        }
        if (homeRuns < 0 || awayRuns < 0) {
            throw new IllegalArgumentException("runs must be >= 0");
        }
        Objects.requireNonNull(half, "half required");
        Objects.requireNonNull(bases, "bases required");
    }

    public int margin() {
        return homeRuns - awayRuns;
    }

    public boolean battingIsHome() {
        return half == InningHalf.BOTTOM;
    }
}
