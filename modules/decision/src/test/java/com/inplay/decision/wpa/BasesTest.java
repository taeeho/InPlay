package com.inplay.decision.wpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BasesTest {

    @Test
    void emptyHasIndexZero() {
        assertThat(Bases.empty().index()).isZero();
        assertThat(Bases.empty().runnersOn()).isZero();
        assertThat(Bases.empty().scoringPosition()).isFalse();
    }

    @Test
    void indexEncodesBasesAsBitmask() {
        assertThat(new Bases(true, false, false).index()).isEqualTo(1);
        assertThat(new Bases(false, true, false).index()).isEqualTo(2);
        assertThat(new Bases(true, true, false).index()).isEqualTo(3);
        assertThat(new Bases(false, false, true).index()).isEqualTo(4);
        assertThat(new Bases(true, false, true).index()).isEqualTo(5);
        assertThat(new Bases(false, true, true).index()).isEqualTo(6);
        assertThat(new Bases(true, true, true).index()).isEqualTo(7);
    }

    @Test
    void loadedDetected() {
        assertThat(Bases.of(1, 1, 1).loaded()).isTrue();
        assertThat(Bases.of(1, 1, 0).loaded()).isFalse();
    }

    @Test
    void scoringPositionIsSecondOrThird() {
        assertThat(new Bases(true, false, false).scoringPosition()).isFalse();
        assertThat(new Bases(false, true, false).scoringPosition()).isTrue();
        assertThat(new Bases(false, false, true).scoringPosition()).isTrue();
    }
}
