package com.inplay.decision.clutch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.inference.clutch.ClutchFeatures;
import com.inplay.inference.clutch.ClutchPredictor;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClutchDetectorTest {

    @Mock ClutchPredictor predictor;

    private final ClutchFeatureBuilder featureBuilder = new ClutchFeatureBuilder();

    private LiveEvent event(int inning, OptionalDouble wpaAfter) {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:34:21Z"),
                new GameId("g1"),
                inning, InningHalf.BOTTOM,
                LiveEventType.PITCH,
                1, new boolean[]{false, true, false},
                new Score(3, 3),
                Optional.empty(), Optional.empty(), Optional.empty(),
                wpaAfter,
                "src", "id");
    }

    @Test
    void nullPredictorGivesModelNotReady() {
        var detector = new ClutchDetector(featureBuilder, null);
        var curr = event(8, OptionalDouble.of(0.62));
        var v = detector.detect(null, curr);
        assertThat(v.reasonCode()).isEqualTo("MODEL_NOT_READY");
        assertThat(v.isClutch()).isFalse();
        assertThat(v.probability()).isNull();
    }

    @Test
    void missingWpaGivesFeaturesUnavailable() {
        var detector = new ClutchDetector(featureBuilder, predictor);
        var curr = event(8, OptionalDouble.empty());
        var v = detector.detect(null, curr);
        assertThat(v.reasonCode()).isEqualTo("FEATURES_UNAVAILABLE");
        Mockito.verifyNoInteractions(predictor);
    }

    @Test
    void readyAboveThresholdReturnsClutch() {
        var detector = new ClutchDetector(featureBuilder, predictor, 0.7);
        Mockito.when(predictor.predict(Mockito.any(ClutchFeatures.class))).thenReturn(0.83);

        var prev = event(8, OptionalDouble.of(0.50));
        var curr = event(8, OptionalDouble.of(0.72));
        var v = detector.detect(prev, curr);

        assertThat(v.isClutch()).isTrue();
        assertThat(v.probability()).isEqualTo(0.83);
        assertThat(v.reasonCode()).isNull();
    }

    @Test
    void belowThresholdReturnsNonClutch() {
        var detector = new ClutchDetector(featureBuilder, predictor, 0.7);
        Mockito.when(predictor.predict(Mockito.any(ClutchFeatures.class))).thenReturn(0.4);

        var prev = event(3, OptionalDouble.of(0.50));
        var curr = event(3, OptionalDouble.of(0.52));
        var v = detector.detect(prev, curr);

        assertThat(v.isClutch()).isFalse();
        assertThat(v.probability()).isEqualTo(0.4);
    }

    @Test
    void defaultThresholdIsZeroPointSeven() {
        var detector = new ClutchDetector(featureBuilder, null);
        var v = detector.detect(null, event(1, OptionalDouble.of(0.5)));
        assertThat(v.threshold()).isEqualTo(0.7);
    }

    @Test
    void rejectsThresholdOutsideUnitInterval() {
        assertThatThrownBy(() -> new ClutchDetector(featureBuilder, null, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClutchDetector(featureBuilder, null, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
