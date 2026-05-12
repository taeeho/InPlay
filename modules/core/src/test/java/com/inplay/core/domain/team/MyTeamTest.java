package com.inplay.core.domain.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MyTeamTest {

    @Test
    void validMyTeamAccepted() {
        Map<KboTeam, Double> weights = Map.of(KboTeam.LG, 1.3, KboTeam.KIA, 1.25, KboTeam.LOTTE, 1.2);
        MyTeam my = new MyTeam(KboTeam.HH, weights);
        assertThat(my.myTeam()).isEqualTo(KboTeam.HH);
        assertThat(my.rivalryWeights()).containsEntry(KboTeam.LG, 1.3);
    }

    @Test
    void selfRivalryRejected() {
        Map<KboTeam, Double> weights = Map.of(KboTeam.HH, 1.5);
        assertThatThrownBy(() -> new MyTeam(KboTeam.HH, weights))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itself");
    }

    @Test
    void nonPositiveWeightRejected() {
        Map<KboTeam, Double> weights = Map.of(KboTeam.LG, 0.0);
        assertThatThrownBy(() -> new MyTeam(KboTeam.HH, weights))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weight");
    }

    @Test
    void nullMyTeamRejected() {
        assertThatThrownBy(() -> new MyTeam(null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rivalryWeightsAreDefensivelyCopied() {
        Map<KboTeam, Double> mutable = new HashMap<>();
        mutable.put(KboTeam.LG, 1.3);
        MyTeam my = new MyTeam(KboTeam.HH, mutable);
        mutable.put(KboTeam.KIA, 999.0);
        assertThat(my.rivalryWeights()).doesNotContainKey(KboTeam.KIA);
    }
}
