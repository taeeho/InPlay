package com.inplay.core.domain.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InningTest {

    @Test
    void validInningAccepted() {
        Inning inning = new Inning(5, InningHalf.BOTTOM);
        assertThat(inning.number()).isEqualTo(5);
        assertThat(inning.half()).isEqualTo(InningHalf.BOTTOM);
    }

    @Test
    void zeroInningRejected() {
        assertThatThrownBy(() -> new Inning(0, InningHalf.TOP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeInningRejected() {
        assertThatThrownBy(() -> new Inning(-1, InningHalf.TOP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullHalfRejected() {
        assertThatThrownBy(() -> new Inning(1, null))
                .isInstanceOf(NullPointerException.class);
    }
}
