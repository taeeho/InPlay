package com.inplay.core.domain.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GameTest {

    private static final GameId ID = new GameId("20260512HHLG");
    private static final LocalDate DATE = LocalDate.of(2026, 5, 12);

    @Test
    void validGameAccepted() {
        Game game = new Game(ID, DATE, KboTeam.HH, KboTeam.LG, GameStatus.SCHEDULED, Score.zero());
        assertThat(game.homeTeam()).isEqualTo(KboTeam.HH);
        assertThat(game.awayTeam()).isEqualTo(KboTeam.LG);
        assertThat(game.status()).isEqualTo(GameStatus.SCHEDULED);
    }

    @Test
    void sameHomeAndAwayRejected() {
        assertThatThrownBy(
                () -> new Game(ID, DATE, KboTeam.HH, KboTeam.HH, GameStatus.SCHEDULED, Score.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("differ");
    }

    @Test
    void nullIdRejected() {
        assertThatThrownBy(
                () -> new Game(null, DATE, KboTeam.HH, KboTeam.LG, GameStatus.SCHEDULED, Score.zero()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullDateRejected() {
        assertThatThrownBy(
                () -> new Game(ID, null, KboTeam.HH, KboTeam.LG, GameStatus.SCHEDULED, Score.zero()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullScoreRejected() {
        assertThatThrownBy(
                () -> new Game(ID, DATE, KboTeam.HH, KboTeam.LG, GameStatus.SCHEDULED, null))
                .isInstanceOf(NullPointerException.class);
    }
}
