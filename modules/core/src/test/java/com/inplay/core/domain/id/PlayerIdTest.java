package com.inplay.core.domain.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlayerIdTest {

    @Test
    void validIdAccepted() {
        assertThat(new PlayerId("ryuhyunjin").value()).isEqualTo("ryuhyunjin");
    }

    @Test
    void blankRejected() {
        assertThatThrownBy(() -> new PlayerId(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
