package com.inplay.decision.clutch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClutchVerdictTest {

    @Test
    void readyAboveThresholdIsClutch() {
        var v = ClutchVerdict.ready(0.85, 0.7);
        assertThat(v.isClutch()).isTrue();
        assertThat(v.probability()).isEqualTo(0.85);
        assertThat(v.reasonCode()).isNull();
    }

    @Test
    void readyBelowThresholdIsNotClutch() {
        var v = ClutchVerdict.ready(0.5, 0.7);
        assertThat(v.isClutch()).isFalse();
    }

    @Test
    void atThresholdIsClutch() {
        var v = ClutchVerdict.ready(0.7, 0.7);
        assertThat(v.isClutch()).isTrue();
    }

    @Test
    void modelNotReadyKeepsNullProbability() {
        var v = ClutchVerdict.modelNotReady(0.7);
        assertThat(v.probability()).isNull();
        assertThat(v.isClutch()).isFalse();
        assertThat(v.reasonCode()).isEqualTo("MODEL_NOT_READY");
    }

    @Test
    void featuresUnavailableSignalsReason() {
        var v = ClutchVerdict.featuresUnavailable(0.7);
        assertThat(v.reasonCode()).isEqualTo("FEATURES_UNAVAILABLE");
        assertThat(v.isClutch()).isFalse();
    }
}
