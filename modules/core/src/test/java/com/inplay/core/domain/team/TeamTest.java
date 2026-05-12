package com.inplay.core.domain.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TeamTest {

    @Test
    void validTeamAccepted() {
        Team team = new Team(KboTeam.HH, "Hanwha Eagles");
        assertThat(team.code()).isEqualTo(KboTeam.HH);
        assertThat(team.fullName()).isEqualTo("Hanwha Eagles");
    }

    @Test
    void nullCodeRejected() {
        assertThatThrownBy(() -> new Team(null, "Hanwha Eagles"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blankFullNameRejected() {
        assertThatThrownBy(() -> new Team(KboTeam.HH, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
