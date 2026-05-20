package com.inplay.decision.brief;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BriefGeneratorTest {

    private final WinProbabilityFeatureBuilder featureBuilder = new WinProbabilityFeatureBuilder();

    @Test
    void nullPredictorYieldsModelNotReadyBrief() {
        var gen = new BriefGenerator(featureBuilder, null);
        Game target = scheduled("g1", LocalDate.of(2026, 5, 20), KboTeam.HH, KboTeam.LG);

        var brief = gen.generate(target, List.of(), KboTeam.HH);

        assertThat(brief.isModelReady()).isFalse();
        assertThat(brief.note()).contains("ONNX");
        assertThat(brief.highlightTeam()).isEqualTo(KboTeam.HH);
    }

    @Test
    void insufficientHistoryYieldsModelNotReadyBrief() {
        var gen = new BriefGenerator(featureBuilder, null);
        Game target = scheduled("g1", LocalDate.of(2026, 5, 20), KboTeam.HH, KboTeam.LG);

        var brief = gen.generate(target, List.of(), KboTeam.LG);

        assertThat(brief.isModelReady()).isFalse();
        assertThat(brief.highlightTeam()).isEqualTo(KboTeam.LG);
    }

    @Test
    void highlightTeamOnlyWhenMyTeamPlays() {
        var gen = new BriefGenerator(featureBuilder, null);
        Game target = scheduled("g1", LocalDate.of(2026, 5, 20), KboTeam.NC, KboTeam.SAMSUNG);

        var brief = gen.generate(target, List.of(), KboTeam.HH);

        assertThat(brief.highlightTeam()).isNull();
    }

    @Test
    void nullMyTeamYieldsNoHighlight() {
        var gen = new BriefGenerator(featureBuilder, null);
        Game target = scheduled("g1", LocalDate.of(2026, 5, 20), KboTeam.HH, KboTeam.LG);

        var brief = gen.generate(target, List.of(), null);

        assertThat(brief.highlightTeam()).isNull();
    }

    private static Game scheduled(String id, LocalDate date, KboTeam home, KboTeam away) {
        return new Game(new GameId(id), date, home, away, GameStatus.SCHEDULED, Score.zero());
    }
}
