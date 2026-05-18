package com.inplay.ingest.game;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GameRepository extends MongoRepository<GameDocument, String> {

    Optional<GameDocument> findByGameId(String gameId);

    List<GameDocument> findByDateBetween(LocalDate from, LocalDate to);
}
