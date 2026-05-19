package com.inplay.inference.winprob;

import java.util.Objects;

/**
 * 승률 예측 모델 입력 feature. Python trainer FEATURE_COLUMNS 와 같은 순서.
 */
public record WinProbabilityFeatures(
        double homeSeasonWinRate,
        double awaySeasonWinRate,
        double homeHomeWinRate,
        double awayAwayWinRate,
        double homeRecent10WinRate,
        double awayRecent10WinRate,
        double h2hHomeWinRate) {

    public static final int FEATURE_COUNT = 7;

    public WinProbabilityFeatures {
        check("homeSeasonWinRate", homeSeasonWinRate);
        check("awaySeasonWinRate", awaySeasonWinRate);
        check("homeHomeWinRate", homeHomeWinRate);
        check("awayAwayWinRate", awayAwayWinRate);
        check("homeRecent10WinRate", homeRecent10WinRate);
        check("awayRecent10WinRate", awayRecent10WinRate);
        check("h2hHomeWinRate", h2hHomeWinRate);
    }

    public float[] toFloatArray() {
        return new float[]{
                (float) homeSeasonWinRate,
                (float) awaySeasonWinRate,
                (float) homeHomeWinRate,
                (float) awayAwayWinRate,
                (float) homeRecent10WinRate,
                (float) awayRecent10WinRate,
                (float) h2hHomeWinRate,
        };
    }

    public static WinProbabilityFeatures fromArray(double[] values) {
        Objects.requireNonNull(values, "values required");
        if (values.length != FEATURE_COUNT) {
            throw new IllegalArgumentException(
                    "expected " + FEATURE_COUNT + " features, got " + values.length);
        }
        return new WinProbabilityFeatures(
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
