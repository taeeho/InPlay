package com.inplay.inference.pitcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PitcherLimitFeaturesTest {

    private PitchSnapshot snap(double seqNorm) {
        return new PitchSnapshot(seqNorm, 0.5, 0.0, 0.5, 0.0, 0.33, 0.5);
    }

    @Test
    void rejectsZeroMaxSeqLen() {
        assertThatThrownBy(() -> new PitcherLimitFeatures(0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tensorShapeIsBatch1xMaxSeqLenx7() {
        var f = new PitcherLimitFeatures(20, List.of(snap(0.1), snap(0.2)));
        float[][][] t = f.toTensor();
        assertThat(t.length).isEqualTo(1);
        assertThat(t[0].length).isEqualTo(20);
        assertThat(t[0][0].length).isEqualTo(7);
    }

    @Test
    void shorterSequenceIsFrontPadded() {
        var f = new PitcherLimitFeatures(5, List.of(snap(0.1), snap(0.2)));
        float[][][] t = f.toTensor();
        // 앞 3 row padded with 0.0
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                assertThat(t[0][i][j]).isEqualTo(0.0f);
            }
        }
        assertThat(t[0][3][0]).isEqualTo(0.1f);
        assertThat(t[0][4][0]).isEqualTo(0.2f);
    }

    @Test
    void longerSequenceIsTrimmedToRecentWindow() {
        List<PitchSnapshot> seq = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            seq.add(snap(i / 10.0));
        }
        var f = new PitcherLimitFeatures(3, seq);
        float[][][] t = f.toTensor();
        // 끝 3개: seqNorm = 0.8, 0.9, 1.0
        assertThat(t[0][0][0]).isCloseTo(0.8f, org.assertj.core.data.Offset.offset(1e-6f));
        assertThat(t[0][2][0]).isCloseTo(1.0f, org.assertj.core.data.Offset.offset(1e-6f));
    }

    @Test
    void emptySequencePadsAllZeros() {
        var f = new PitcherLimitFeatures(4, List.of());
        float[][][] t = f.toTensor();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 7; j++) {
                assertThat(t[0][i][j]).isEqualTo(0.0f);
            }
        }
    }
}
