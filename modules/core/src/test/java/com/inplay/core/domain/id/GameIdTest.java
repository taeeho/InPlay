package com.inplay.core.domain.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GameIdTest {

    @Test
    void validIdAccepted() {
        GameId id = new GameId("20260512HHLG");
        assertThat(id.value()).isEqualTo("20260512HHLG");
    }

    @Test
    void blankRejected() {
        assertThatThrownBy(() -> new GameId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> new GameId(null))
                .isInstanceOf(NullPointerException.class);
    }
}
