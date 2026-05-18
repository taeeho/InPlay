package com.inplay.ingest.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GameMapperTest {

    private static Game sampleGame() {
        return new Game(
                new GameId("20260512HHLG"),
                LocalDate.of(2026, 5, 12),
                KboTeam.HH,
                KboTeam.LG,
                GameStatus.FINAL,
                new Score(4, 3));
    }

    @Test
    void toDocumentMapsAllFields() {
        var doc = GameMapper.toDocument(sampleGame());
        assertThat(doc.id()).isNull();
        assertThat(doc.gameId()).isEqualTo("20260512HHLG");
        assertThat(doc.date()).isEqualTo(LocalDate.of(2026, 5, 12));
        assertThat(doc.homeTeam()).isEqualTo("HH");
        assertThat(doc.awayTeam()).isEqualTo("LG");
        assertThat(doc.status()).isEqualTo("FINAL");
        assertThat(doc.score().home()).isEqualTo(4);
        assertThat(doc.score().away()).isEqualTo(3);
    }

    @Test
    void toDomainMapsAllFields() {
        var doc = new GameDocument(
                "ObjectId-stub",
                "20260512HHLG",
                LocalDate.of(2026, 5, 12),
                "HH",
                "LG",
                "FINAL",
                new GameDocument.ScoreDocument(4, 3));
        var game = GameMapper.toDomain(doc);
        assertThat(game.id().value()).isEqualTo("20260512HHLG");
        assertThat(game.homeTeam()).isEqualTo(KboTeam.HH);
        assertThat(game.awayTeam()).isEqualTo(KboTeam.LG);
        assertThat(game.status()).isEqualTo(GameStatus.FINAL);
        assertThat(game.score().home()).isEqualTo(4);
    }

    @Test
    void roundTripPreservesValues() {
        Game original = sampleGame();
        Game roundtripped = GameMapper.toDomain(GameMapper.toDocument(original));
        assertThat(roundtripped).isEqualTo(original);
    }

    @Test
    void unknownTeamCodeRejected() {
        var doc = new GameDocument(
                null, "20260512HHLG", LocalDate.of(2026, 5, 12),
                "ZZZ", "LG", "FINAL", new GameDocument.ScoreDocument(0, 0));
        assertThatThrownBy(() -> GameMapper.toDomain(doc))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ZZZ");
    }

    @Test
    void withIdReplacesIdOnly() {
        var doc = GameMapper.toDocument(sampleGame());
        var withId = doc.withId("oid-1");
        assertThat(withId.id()).isEqualTo("oid-1");
        assertThat(withId.gameId()).isEqualTo(doc.gameId());
        assertThat(withId.score()).isEqualTo(doc.score());
    }
}
