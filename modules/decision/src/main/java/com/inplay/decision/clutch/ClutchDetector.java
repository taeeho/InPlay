package com.inplay.decision.clutch;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.inference.clutch.ClutchPredictor;

import java.util.Objects;

/**
 * LiveEvent를 받아 ClutchVerdict 생성.
 *
 * <p>predictor null (ONNX 미산출) → MODEL_NOT_READY verdict, isClutch=false → 알림 없음.
 * 다음 W4 단계에서 importance score(rivalry × leverage) + cooldown · dedupe와 결합.
 *
 * <p>threshold 기본값 0.7 (PLAN.md §8 W4 Gate "본인 평가 precision ≥ 0.7" 정렬).
 */
public final class ClutchDetector {

    public static final double DEFAULT_THRESHOLD = 0.7;

    private final ClutchFeatureBuilder featureBuilder;
    private final ClutchPredictor predictor;
    private final double threshold;

    public ClutchDetector(ClutchFeatureBuilder featureBuilder,
                          ClutchPredictor predictor,
                          double threshold) {
        this.featureBuilder = Objects.requireNonNull(featureBuilder, "featureBuilder required");
        this.predictor = predictor;
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be in [0,1], got " + threshold);
        }
        this.threshold = threshold;
    }

    public ClutchDetector(ClutchFeatureBuilder featureBuilder, ClutchPredictor predictor) {
        this(featureBuilder, predictor, DEFAULT_THRESHOLD);
    }

    public ClutchVerdict detect(LiveEvent previous, LiveEvent current) {
        Objects.requireNonNull(current, "current required");
        if (predictor == null) {
            return ClutchVerdict.modelNotReady(threshold);
        }
        var features = featureBuilder.build(previous, current);
        if (features.isEmpty()) {
            return ClutchVerdict.featuresUnavailable(threshold);
        }
        double probability = predictor.predict(features.get());
        return ClutchVerdict.ready(probability, threshold);
    }
}
