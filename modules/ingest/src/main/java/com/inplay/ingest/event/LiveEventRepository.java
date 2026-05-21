package com.inplay.ingest.event;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface LiveEventRepository extends MongoRepository<LiveEventDocument, String> {

    @Query("{ 'meta.game_id': ?0 }")
    List<LiveEventDocument> findByGameId(String gameId);

    @Query(value = "{ 'meta.game_id': ?0 }", count = true)
    long countByGameId(String gameId);
}
