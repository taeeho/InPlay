package com.inplay.decision.brief;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BriefFormatterTest {

    @Test
    void readyBriefShowsProbabilities() {
        var brief = new WinProbabilityBrief(
                new GameId("g1"), LocalDate.of(2026, 5, 20),
                KboTeam.HH, KboTeam.LG, 0.62, KboTeam.HH, null);

        String text = BriefFormatter.format(brief);

        assertThat(text).contains("KBO");
        assertThat(text).contains("**HH**");
        assertThat(text).contains("LG");
        assertThat(text).contains("62.0%");
        assertThat(text).contains("38.0%");
    }

    @Test
    void notReadyBriefShowsNote() {
        var brief = new WinProbabilityBrief(
                new GameId("g1"), LocalDate.of(2026, 5, 20),
                KboTeam.HH, KboTeam.LG, null, null, "데이터 부족");

        String text = BriefFormatter.format(brief);

        assertThat(text).contains("승률: —");
        assertThat(text).contains("데이터 부족");
    }

    @Test
    void noHighlightWhenMyTeamAbsent() {
        var brief = new WinProbabilityBrief(
                new GameId("g1"), LocalDate.of(2026, 5, 20),
                KboTeam.NC, KboTeam.SAMSUNG, 0.5, null, null);

        String text = BriefFormatter.format(brief);

        assertThat(text).doesNotContain("**NC**");
        assertThat(text).doesNotContain("**SAMSUNG**");
    }
}
