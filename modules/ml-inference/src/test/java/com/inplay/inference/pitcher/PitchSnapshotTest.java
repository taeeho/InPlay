package com.inplay.inference.pitcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PitchSnapshotTest {

    @Test
    void toFloatArrayPreservesOrder() {
        var s = new PitchSnapshot(0.1, 0.5, 0.0, 0.7, 0.0, 0.33, 0.5);
        float[] arr = s.toFloatArray();
        assertThat(arr).hasSize(7);
        assertThat(arr[0]).isEqualTo(0.1f);
        assertThat(arr[6]).isEqualTo(0.5f);
    }

    @Test
    void rejectsOutOfRange() {
        assertThatThrownBy(() -> new PitchSnapshot(1.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[0,1]");
    }

    @Test
    void rejectsNaN() {
        assertThatThrownBy(() -> new PitchSnapshot(Double.NaN, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    void boundaryValuesAllowed() {
        var s = new PitchSnapshot(0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0);
        assertThat(s.toFloatArray()).hasSize(7);
    }
}
