package com.inplay.core.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class UserMuteWindowTest {

    @Test
    void disabledIsRecognized() {
        assertThat(UserMuteWindow.disabled().isDisabled()).isTrue();
    }

    @Test
    void normalWindowIsNotDisabled() {
        var w = new UserMuteWindow(LocalTime.of(8, 0), LocalTime.of(10, 0), ZoneId.of("Asia/Seoul"));
        assertThat(w.isDisabled()).isFalse();
    }

    @Test
    void wrapAroundWindowAlsoActive() {
        var w = new UserMuteWindow(LocalTime.of(23, 0), LocalTime.of(7, 0), ZoneId.of("Asia/Seoul"));
        assertThat(w.isDisabled()).isFalse();
    }
}
