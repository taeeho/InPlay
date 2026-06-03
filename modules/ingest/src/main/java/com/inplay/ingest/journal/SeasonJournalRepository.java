package com.inplay.ingest.journal;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SeasonJournalRepository extends MongoRepository<SeasonJournalDocument, String> {

    boolean existsByUserIdAndSeasonAndGameId(String userId, int season, String gameId);
}
