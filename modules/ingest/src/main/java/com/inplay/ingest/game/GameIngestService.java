package com.inplay.ingest.game;

import com.inplay.core.domain.game.Game;
import java.util.List;
import java.util.Objects;

public class GameIngestService {

    private final GameRepository repository;

    public GameIngestService(GameRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository required");
    }

    public int upsertAll(List<Game> games) {
        Objects.requireNonNull(games, "games required");
        int count = 0;
        for (Game game : games) {
            upsert(game);
            count++;
        }
        return count;
    }

    public GameDocument upsert(Game game) {
        Objects.requireNonNull(game, "game required");
        GameDocument incoming = GameMapper.toDocument(game);
        return repository.findByGameId(game.id().value())
                .map(existing -> repository.save(incoming.withId(existing.id())))
                .orElseGet(() -> repository.save(incoming));
    }
}
