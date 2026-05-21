package com.inplay.inference.clutch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClutchFeaturesTest {

    @Test
    void toFloatArrayPreservesOrder() {
        var f = new ClutchFeatures(0.35, 0.6, 0.8, 0.2, 0.66, 0.33, 0.42);
        float[] arr = f.toFloatArray();
        assertThat(arr).hasSize(7);
        assertThat(arr[0]).isEqualTo(0.35f);
        assertThat(arr[6]).isEqualTo(0.42f);
    }

    @Test
    void fromArrayRoundTrip() {
        double[] values = {0.1, 0.2, 0.9, 0.3, 0.66, 0.0, 0.18};
        var f = ClutchFeatures.fromArray(values);
        float[] arr = f.toFloatArray();
        for (int i = 0; i < values.length; i++) {
            assertThat((double) arr[i]).isCloseTo(values[i], org.assertj.core.data.Offset.offset(1e-6));
        }
    }

    @Test
    void rejectsWrongLength() {
        assertThatThrownBy(() -> ClutchFeatures.fromArray(new double[]{0.5, 0.5}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7");
    }

    @Test
    void rejectsOutOfRangeNormalizedFeature() {
        assertThatThrownBy(() ->
                new ClutchFeatures(1.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[0,1]");
    }

    @Test
    void leverageProxyAllowsUpTo1_5() {
        // wpa_change_abs(1.0) * inning_progress(1.0) * (1 + 0.5*runners_on_norm(1.0)) = 1.5
        var f = new ClutchFeatures(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.5);
        assertThat(f.toFloatArray()[6]).isEqualTo(1.5f);
    }

    @Test
    void leverageProxyRejectsAbove1_5() {
        assertThatThrownBy(() ->
                new ClutchFeatures(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 1.6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leverageProxy");
    }

    @Test
    void rejectsNaN() {
        assertThatThrownBy(() ->
                new ClutchFeatures(Double.NaN, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }
}
