package com.inplay.decision.clutch;

/**
 * Clutch detector verdict for a single LiveEvent.
 *
 * @param probability clutch 확률 ∈ [0,1]. predictor 없으면 null.
 * @param threshold   알림 발사 cutoff (보통 0.7, 사용자 설정 가능).
 * @param isClutch    probability ≥ threshold 인가. predictor null 이면 false.
 * @param reasonCode  특수 상황 표시(MODEL_NOT_READY / FEATURES_UNAVAILABLE). 평상시 null.
 */
public record ClutchVerdict(
        Double probability,
        double threshold,
        boolean isClutch,
        String reasonCode
) {
    public static ClutchVerdict ready(double probability, double threshold) {
        return new ClutchVerdict(probability, threshold, probability >= threshold, null);
    }

    public static ClutchVerdict modelNotReady(double threshold) {
        return new ClutchVerdict(null, threshold, false, "MODEL_NOT_READY");
    }

    public static ClutchVerdict featuresUnavailable(double threshold) {
        return new ClutchVerdict(null, threshold, false, "FEATURES_UNAVAILABLE");
    }
}
