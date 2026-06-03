package com.inplay.journal.season;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JournalEntryGeneratorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 3);
    private final JournalEntryGenerator generator = new JournalEntryGenerator();

    private Game game(KboTeam home, KboTeam away, int h, int a, GameStatus status) {
        return new Game(new GameId("g1"), DATE, home, away, status, new Score(h, a));
    }

    @Test
    void homeMyTeamWin() {
        Optional<SeasonJournalEntry> e =
                generator.generate(game(KboTeam.HH, KboTeam.LG, 5, 3, GameStatus.FINAL), KboTeam.HH, "taeeho");

        assertThat(e).isPresent();
        assertThat(e.get().outcome()).isEqualTo(JournalOutcome.WIN);
        assertThat(e.get().season()).isEqualTo(2026);
        assertThat(e.get().summary()).isEqualTo("HH 5:3 LG");
    }

    @Test
    void awayMyTeamWin() {
        Optional<SeasonJournalEntry> e =
                generator.generate(game(KboTeam.LG, KboTeam.HH, 3, 5, GameStatus.FINAL), KboTeam.HH, "taeeho");

        assertThat(e.get().outcome()).isEqualTo(JournalOutcome.WIN);
    }

    @Test
    void myTeamLoss() {
        Optional<SeasonJournalEntry> e =
                generator.generate(game(KboTeam.HH, KboTeam.LG, 2, 7, GameStatus.FINAL), KboTeam.HH, "taeeho");

        assertThat(e.get().outcome()).isEqualTo(JournalOutcome.LOSS);
    }

    @Test
    void drawWhenTied() {
        Optional<SeasonJournalEntry> e =
                generator.generate(game(KboTeam.HH, KboTeam.LG, 4, 4, GameStatus.FINAL), KboTeam.HH, "taeeho");

        assertThat(e.get().outcome()).isEqualTo(JournalOutcome.DRAW);
    }

    @Test
    void emptyWhenNotFinal() {
        assertThat(generator.generate(game(KboTeam.HH, KboTeam.LG, 0, 0, GameStatus.LIVE), KboTeam.HH, "taeeho"))
                .isEmpty();
    }

    @Test
    void emptyWhenMyTeamAbsent() {
        assertThat(generator.generate(game(KboTeam.KIA, KboTeam.SSG, 1, 0, GameStatus.FINAL), KboTeam.HH, "taeeho"))
                .isEmpty();
    }
}
