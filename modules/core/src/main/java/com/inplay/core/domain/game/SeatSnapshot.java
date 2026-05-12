package com.inplay.core.domain.game;

import java.util.Objects;

public record SeatSnapshot(
        Inning inning,
        int outs,
        boolean runnerOnFirst,
        boolean runnerOnSecond,
        boolean runnerOnThird,
        Count count) {
    public SeatSnapshot {
        Objects.requireNonNull(inning, "inning required");
        Objects.requireNonNull(count, "count required");
        if (outs < 0 || outs > 2) {
            throw new IllegalArgumentException("outs must be in 0..2, got " + outs);
        }
    }

    public boolean basesLoaded() {
        return runnerOnFirst && runnerOnSecond && runnerOnThird;
    }

    public int runnersOnBase() {
        int total = 0;
        if (runnerOnFirst) total++;
        if (runnerOnSecond) total++;
        if (runnerOnThird) total++;
        return total;
    }
}
