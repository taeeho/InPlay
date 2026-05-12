package com.inplay.core.domain.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CountTest {

    @Test
    void freshCountIsZeroZero() {
        assertThat(Count.fresh()).isEqualTo(new Count(0, 0));
    }

    @Test
    void fullCountAccepted() {
        Count c = new Count(3, 2);
        assertThat(c.balls()).isEqualTo(3);
        assertThat(c.strikes()).isEqualTo(2);
    }

    @Test
    void ballsAboveThreeRejected() {
        assertThatThrownBy(() -> new Count(4, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void strikesAboveTwoRejected() {
        assertThatThrownBy(() -> new Count(0, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeBallsRejected() {
        assertThatThrownBy(() -> new Count(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeStrikesRejected() {
        assertThatThrownBy(() -> new Count(0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
