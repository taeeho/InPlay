package com.inplay.decision.wpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RunExpectancyTest {

    @Test
    void emptyZeroOutsBaseline() {
        // Standard FanGraphs RE24 — empty 0 outs ≈ 0.48
        double re = RunExpectancy.expectedRuns(Bases.empty(), 0);
        assertThat(re).isCloseTo(0.481, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void loadedZeroOutsHighest() {
        double loaded = RunExpectancy.expectedRuns(Bases.of(1, 1, 1), 0);
        double empty = RunExpectancy.expectedRuns(Bases.empty(), 0);
        assertThat(loaded).isGreaterThan(empty);
        assertThat(loaded).isGreaterThan(2.0);
    }

    @Test
    void moreOutsDecreasesExpectedRuns() {
        for (int b = 0; b < 8; b++) {
            Bases bases = new Bases((b & 1) > 0, (b & 2) > 0, (b & 4) > 0);
            double re0 = RunExpectancy.expectedRuns(bases, 0);
            double re1 = RunExpectancy.expectedRuns(bases, 1);
            double re2 = RunExpectancy.expectedRuns(bases, 2);
            assertThat(re0).as("RE 0o vs 1o for bases %s", bases).isGreaterThan(re1);
            assertThat(re1).as("RE 1o vs 2o for bases %s", bases).isGreaterThan(re2);
        }
    }

    @Test
    void threeOutsHalfInningOver() {
        assertThat(RunExpectancy.expectedRuns(Bases.of(1, 1, 1), 3)).isZero();
    }
}
