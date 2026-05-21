package com.inplay.inference.clutch;

import java.util.Objects;

/**
 * Clutch(결정적 순간) 분류 모델 입력 feature. Python trainer FEATURE_COLUMNS 와 같은 순서.
 *
 * <p>모든 feature는 [0,1] 범위로 정규화 (trainer/clutch/features.py 참조).
 */
public record ClutchFeatures(
        double wpaChangeAbs,
        double weBalance,
        double inningProgress,
        double scoreMarginNorm,
        double runnersOnNorm,
        double outsNorm,
        double leverageProxy) {

    public static final int FEATURE_COUNT = 7;

    public ClutchFeatures {
        check("wpaChangeAbs", wpaChangeAbs);
        check("weBalance", weBalance);
        check("inningProgress", inningProgress);
        check("scoreMarginNorm", scoreMarginNorm);
        check("runnersOnNorm", runnersOnNorm);
        check("outsNorm", outsNorm);
        // leverage_proxy는 wpa_change_abs × inning_progress × (1 + 0.5*runners_on_norm)
        // → 최대값 1 × 1 × 1.5 = 1.5. [0, 1.5] 허용.
        if (Double.isNaN(leverageProxy) || Double.isInfinite(leverageProxy)
                || leverageProxy < 0.0 || leverageProxy > 1.5) {
            throw new IllegalArgumentException("leverageProxy must be in [0, 1.5], got " + leverageProxy);
        }
    }

    public float[] toFloatArray() {
        return new float[]{
                (float) wpaChangeAbs,
                (float) weBalance,
                (float) inningProgress,
                (float) scoreMarginNorm,
                (float) runnersOnNorm,
                (float) outsNorm,
                (float) leverageProxy,
        };
    }

    public static ClutchFeatures fromArray(double[] values) {
        Objects.requireNonNull(values, "values required");
        if (values.length != FEATURE_COUNT) {
            throw new IllegalArgumentException(
                    "expected " + FEATURE_COUNT + " features, got " + values.length);
        }
        return new ClutchFeatures(
                values[0], values[1], values[2], values[3],
                values[4], values[5], values[6]);
    }

    private static void check(String name, double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException(name + " must be finite, got " + v);
        }
        if (v < 0.0 || v > 1.0) {
            throw new IllegalArgumentException(name + " must be in [0,1], got " + v);
        }
    }
}
