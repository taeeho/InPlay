package com.inplay.core.domain.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ScoreTest {

    @Test
    void zeroScoreAccepted() {
        assertThat(Score.zero()).isEqualTo(new Score(0, 0));
    }

    @Test
    void marginIsHomeMinusAway() {
        assertThat(new Score(4, 3).margin()).isEqualTo(1);
        assertThat(new Score(2, 5).margin()).isEqualTo(-3);
    }

    @Test
    void negativeHomeRejected() {
        assertThatThrownBy(() -> new Score(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeAwayRejected() {
        assertThatThrownBy(() -> new Score(0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
