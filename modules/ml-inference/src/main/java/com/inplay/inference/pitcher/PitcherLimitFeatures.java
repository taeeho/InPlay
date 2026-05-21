package com.inplay.inference.pitcher;

import java.util.List;
import java.util.Objects;

/**
 * LSTM 입력 — 한 outing의 pitch sequence (정규화 후 padding).
 *
 * <p>Python {@code pitcher_limit/features.py} 의 {@code build_sequences} 와 동일 규칙:
 * <ul>
 *   <li>seq_len > maxSeqLen → 끝 maxSeqLen 만 사용 (recent window)</li>
 *   <li>seq_len < maxSeqLen → 앞쪽을 0.0 padding</li>
 * </ul>
 */
public record PitcherLimitFeatures(int maxSeqLen, List<PitchSnapshot> sequence) {

    public PitcherLimitFeatures {
        if (maxSeqLen < 1) {
            throw new IllegalArgumentException("maxSeqLen must be >= 1, got " + maxSeqLen);
        }
        Objects.requireNonNull(sequence, "sequence required");
        sequence = List.copyOf(sequence);
    }

    /**
     * @return shape [1, maxSeqLen, PitchSnapshot.FEATURE_COUNT] — front-padded with 0.0.
     */
    public float[][][] toTensor() {
        float[][][] tensor = new float[1][maxSeqLen][PitchSnapshot.FEATURE_COUNT];
        int n = sequence.size();
        int writeStart;
        int readStart;
        if (n >= maxSeqLen) {
            // 끝 maxSeqLen 만 사용
            writeStart = 0;
            readStart = n - maxSeqLen;
            for (int i = 0; i < maxSeqLen; i++) {
                tensor[0][writeStart + i] = sequence.get(readStart + i).toFloatArray();
            }
        } else {
            // 앞쪽 padding (이미 0.0으로 초기화됨)
            writeStart = maxSeqLen - n;
            for (int i = 0; i < n; i++) {
                tensor[0][writeStart + i] = sequence.get(i).toFloatArray();
            }
        }
        return tensor;
    }
}
