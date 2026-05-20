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

class WinProbabilityFeatureBuilderTest {

    private final WinProbabilityFeatureBuilder builder = new WinProbabilityFeatureBuilder();

    @Test
    void returnsEmptyWhenHistoryTooSmall() {
        Game target = scheduled("g1", LocalDate.of(2026, 5, 20), KboTeam.HH, KboTeam.LG);
        assertThat(builder.build(target, List.of())).isEmpty();
    }

    @Test
    void excludesFutureGamesAndNonFinal() {
        LocalDate target = LocalDate.of(2026, 5, 20);
        List<Game> history = List.of(
                finalGame("p1", target.minusDays(10), KboTeam.HH, KboTeam.KIA, 5, 3),
                finalGame("p2", target.minusDays(8), KboTeam.LG, KboTeam.HH, 2, 4),
                finalGame("p3", target.minusDays(5), KboTeam.HH, KboTeam.LG, 1, 6),
                finalGame("p4", target.minusDays(3), KboTeam.LG, KboTeam.SSG, 7, 2),
                finalGame("p5", target.minusDays(1), KboTeam.KIA, KboTeam.LG, 4, 3),
                scheduled("future", target.plusDays(1), KboTeam.HH, KboTeam.LG));

        var features = builder.build(scheduled("t", target, KboTeam.HH, KboTeam.LG), history);

        assertThat(features).isPresent();
        var f = features.get();
        // HH: 3 games, won 2 (p1 win, p2 win, p3 lose) → 0.667
        assertThat(f.homeSeasonWinRate()).isCloseTo(2.0 / 3, within(1e-6));
        // LG: 4 games, won 2 (p2 lose, p3 win, p4 win, p5 lose) → 0.5
        assertThat(f.awaySeasonWinRate()).isCloseTo(0.5, within(1e-6));
        // h2h: p2 (LG home, HH won) + p3 (HH home, LG won) → HH 1 of 2 = 0.5
        assertThat(f.h2hHomeWinRate()).isCloseTo(0.5, within(1e-6));
    }

    @Test
    void tieDoesNotCountAsWin() {
        LocalDate target = LocalDate.of(2026, 5, 20);
        List<Game> history = List.of(
                finalGame("p1", target.minusDays(5), KboTeam.HH, KboTeam.LG, 3, 3),
                finalGame("p2", target.minusDays(4), KboTeam.HH, KboTeam.KIA, 5, 4),
                finalGame("p3", target.minusDays(3), KboTeam.HH, KboTeam.SSG, 2, 6),
                finalGame("p4", target.minusDays(5), KboTeam.LG, KboTeam.NC, 1, 5),
                finalGame("p5", target.minusDays(4), KboTeam.LG, KboTeam.KIA, 3, 2),
                finalGame("p6", target.minusDays(3), KboTeam.LG, KboTeam.SSG, 7, 0));

        var features = builder.build(scheduled("t", target, KboTeam.HH, KboTeam.LG), history);
        assertThat(features).isPresent();
        // HH 3 games: tie + win + lose → 1 win / 3 = 0.333 (tie ≠ win)
        assertThat(features.get().homeSeasonWinRate()).isCloseTo(1.0 / 3, within(1e-6));
    }

    private static org.assertj.core.data.Offset<Double> within(double v) {
        return org.assertj.core.data.Offset.offset(v);
    }

    private static Game scheduled(String id, LocalDate date, KboTeam home, KboTeam away) {
        return new Game(new GameId(id), date, home, away, GameStatus.SCHEDULED, Score.zero());
    }

    private static Game finalGame(String id, LocalDate date, KboTeam home, KboTeam away, int hs, int as) {
        return new Game(new GameId(id), date, home, away, GameStatus.FINAL, new Score(hs, as));
    }
}
