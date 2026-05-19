package com.inplay.inference.winprob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WinProbabilityFeaturesTest {

    @Test
    void toFloatArrayPreservesOrder() {
        var f = new WinProbabilityFeatures(0.55, 0.45, 0.60, 0.40, 0.50, 0.50, 0.52);
        float[] arr = f.toFloatArray();
        assertThat(arr).hasSize(7);
        assertThat(arr[0]).isEqualTo(0.55f);
        assertThat(arr[6]).isEqualTo(0.52f);
    }

    @Test
    void fromArrayRoundTrip() {
        double[] values = {0.5, 0.5, 0.6, 0.4, 0.55, 0.45, 0.52};
        var f = WinProbabilityFeatures.fromArray(values);
        float[] arr = f.toFloatArray();
        for (int i = 0; i < values.length; i++) {
            assertThat((double) arr[i]).isCloseTo(values[i], org.assertj.core.data.Offset.offset(1e-6));
        }
    }

    @Test
    void rejectsWrongLength() {
        assertThatThrownBy(() -> WinProbabilityFeatures.fromArray(new double[]{0.5, 0.5}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7");
    }

    @Test
    void rejectsOutOfRange() {
        assertThatThrownBy(() ->
                new WinProbabilityFeatures(1.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[0,1]");
    }

    @Test
    void rejectsNaN() {
        assertThatThrownBy(() ->
                new WinProbabilityFeatures(Double.NaN, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    void boundaryValuesAllowed() {
        var f = new WinProbabilityFeatures(0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.5);
        assertThat(f.toFloatArray()).hasSize(7);
    }
}
