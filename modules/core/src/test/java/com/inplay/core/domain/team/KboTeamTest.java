package com.inplay.core.domain.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KboTeamTest {

    @Test
    void hasExactlyTenTeams() {
        assertThat(KboTeam.values()).hasSize(10);
    }

    @Test
    void allExpectedCodesPresent() {
        assertThat(KboTeam.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder(
                        "HH", "LG", "KIA", "SSG", "KT", "KIWOOM", "NC", "SAMSUNG", "LOTTE", "HAN");
    }

    @Test
    void fromCodeReturnsMatchingTeam() {
        assertThat(KboTeam.fromCode("HH")).isEqualTo(KboTeam.HH);
        assertThat(KboTeam.fromCode("KIWOOM")).isEqualTo(KboTeam.KIWOOM);
    }

    @Test
    void fromCodeUnknownRejected() {
        assertThatThrownBy(() -> KboTeam.fromCode("DOOSAN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }
}
