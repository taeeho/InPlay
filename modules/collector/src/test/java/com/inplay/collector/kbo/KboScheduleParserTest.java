package com.inplay.collector.kbo;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class KboScheduleParserTest {

    private final KboScheduleParser parser = new KboScheduleParser();

    @Test
    void parsesSingleFinalGame() {
        String html = """
                <table class="schedule">
                  <tr class="game" data-game-id="20260512HHLG" data-date="2026-05-12">
                    <td class="home">HH</td>
                    <td class="score">4-3</td>
                    <td class="away">LG</td>
                    <td class="status">FINAL</td>
                  </tr>
                </table>
                """;
        var games = parser.parse(html);
        assertThat(games).hasSize(1);
        var g = games.get(0);
        assertThat(g.id().value()).isEqualTo("20260512HHLG");
        assertThat(g.date()).isEqualTo(LocalDate.of(2026, 5, 12));
        assertThat(g.homeTeam()).isEqualTo(KboTeam.HH);
        assertThat(g.awayTeam()).isEqualTo(KboTeam.LG);
        assertThat(g.status()).isEqualTo(GameStatus.FINAL);
        assertThat(g.score().home()).isEqualTo(4);
        assertThat(g.score().away()).isEqualTo(3);
    }

    @Test
    void parsesScheduledGameWithoutScore() {
        String html = """
                <table class="schedule">
                  <tr class="game" data-game-id="20260513HHLG" data-date="2026-05-13">
                    <td class="home">HH</td>
                    <td class="score"></td>
                    <td class="away">LG</td>
                    <td class="status">SCHEDULED</td>
                  </tr>
                </table>
                """;
        var games = parser.parse(html);
        assertThat(games).hasSize(1);
        assertThat(games.get(0).score().home()).isZero();
        assertThat(games.get(0).score().away()).isZero();
        assertThat(games.get(0).status()).isEqualTo(GameStatus.SCHEDULED);
    }

    @Test
    void parsesMultipleGames() {
        String html = """
                <table class="schedule">
                  <tr class="game" data-game-id="20260512HHLG" data-date="2026-05-12">
                    <td class="home">HH</td><td class="score">4-3</td><td class="away">LG</td><td class="status">FINAL</td>
                  </tr>
                  <tr class="game" data-game-id="20260512KIASSG" data-date="2026-05-12">
                    <td class="home">KIA</td><td class="score">2-5</td><td class="away">SSG</td><td class="status">FINAL</td>
                  </tr>
                </table>
                """;
        var games = parser.parse(html);
        assertThat(games).hasSize(2);
        assertThat(games).extracting(g -> g.id().value())
                .containsExactly("20260512HHLG", "20260512KIASSG");
    }

    @Test
    void emptyHtmlReturnsEmpty() {
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void skipsRowsMissingRequiredFields() {
        String html = """
                <table class="schedule">
                  <tr class="game" data-game-id="20260512HHLG" data-date="2026-05-12">
                    <td class="home">HH</td><td class="score">4-3</td><td class="away">LG</td><td class="status">FINAL</td>
                  </tr>
                  <tr class="game" data-game-id="" data-date="2026-05-12">
                    <td class="home">HH</td><td class="score">4-3</td><td class="away">LG</td><td class="status">FINAL</td>
                  </tr>
                </table>
                """;
        var games = parser.parse(html);
        assertThat(games).hasSize(1);
    }
}
