package com.inplay.core.domain.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.core.domain.id.PlayerId;
import com.inplay.core.domain.team.KboTeam;
import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void validPlayerAccepted() {
        Player p = new Player(new PlayerId("ryuhyunjin"), KboTeam.HH, "Ryu Hyun-jin");
        assertThat(p.team()).isEqualTo(KboTeam.HH);
        assertThat(p.name()).isEqualTo("Ryu Hyun-jin");
    }

    @Test
    void nullIdRejected() {
        assertThatThrownBy(() -> new Player(null, KboTeam.HH, "Ryu"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullTeamRejected() {
        assertThatThrownBy(() -> new Player(new PlayerId("p1"), null, "Ryu"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blankNameRejected() {
        assertThatThrownBy(() -> new Player(new PlayerId("p1"), KboTeam.HH, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
