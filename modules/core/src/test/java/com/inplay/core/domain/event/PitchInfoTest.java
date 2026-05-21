package com.inplay.core.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PitchInfoTest {

    @Test
    void buildsHappyPath() {
        var p = new PitchInfo("FF", 147, "strike");
        assertThat(p.type()).isEqualTo("FF");
        assertThat(p.speedKmh()).isEqualTo(147);
    }

    @Test
    void rejectsBlankType() {
        assertThatThrownBy(() -> new PitchInfo("", 140, "ball")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankResult() {
        assertThatThrownBy(() -> new PitchInfo("CU", 130, "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnrealisticSpeed() {
        assertThatThrownBy(() -> new PitchInfo("FF", 201, "strike")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PitchInfo("FF", -1, "strike")).isInstanceOf(IllegalArgumentException.class);
    }
}
