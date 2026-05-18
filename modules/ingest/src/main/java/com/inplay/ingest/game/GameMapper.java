package com.inplay.ingest.game;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.util.Objects;

public final class GameMapper {

    private GameMapper() {}

    public static GameDocument toDocument(Game game) {
        Objects.requireNonNull(game, "game required");
        return GameDocument.forNew(
                game.id().value(),
                game.date(),
                game.homeTeam().code(),
                game.awayTeam().code(),
                game.status().name(),
                new GameDocument.ScoreDocument(game.score().home(), game.score().away()));
    }

    public static Game toDomain(GameDocument doc) {
        Objects.requireNonNull(doc, "doc required");
        return new Game(
                new GameId(doc.gameId()),
                doc.date(),
                KboTeam.fromCode(doc.homeTeam()),
                KboTeam.fromCode(doc.awayTeam()),
                GameStatus.valueOf(doc.status()),
                new Score(doc.score().home(), doc.score().away()));
    }
}
