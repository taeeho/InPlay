package com.inplay.inference.pitcher;

/**
 * 단일 pitch의 정규화 feature. Python trainer pitcher_limit/features.py FEATURE_COLUMNS 와 같은 순서.
 *
 * <p>모든 값은 [0,1] 정규화 (PitcherLimitFeatures 가 invariant 검증).
 */
public record PitchSnapshot(
        double pitchSeqNorm,
        double pitchSpeedNorm,
        double pitchTypeNorm,
        double inningNorm,
        double outsNorm,
        double runnersOnNorm,
        double lineupPosNorm) {

    public static final int FEATURE_COUNT = 7;

    public PitchSnapshot {
        check("pitchSeqNorm", pitchSeqNorm);
        check("pitchSpeedNorm", pitchSpeedNorm);
        check("pitchTypeNorm", pitchTypeNorm);
        check("inningNorm", inningNorm);
        check("outsNorm", outsNorm);
        check("runnersOnNorm", runnersOnNorm);
        check("lineupPosNorm", lineupPosNorm);
    }

    public float[] toFloatArray() {
        return new float[]{
                (float) pitchSeqNorm, (float) pitchSpeedNorm, (float) pitchTypeNorm,
                (float) inningNorm, (float) outsNorm, (float) runnersOnNorm, (float) lineupPosNorm,
        };
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
