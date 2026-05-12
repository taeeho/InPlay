package com.inplay.core.domain.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserIdTest {

    @Test
    void validIdAccepted() {
        assertThat(new UserId("u_taeeho").value()).isEqualTo("u_taeeho");
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(NullPointerException.class);
    }
}
