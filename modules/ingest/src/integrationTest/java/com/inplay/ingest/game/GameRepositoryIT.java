package com.inplay.ingest.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@Testcontainers
@DataMongoTest
class GameRepositoryIT {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }

    @Autowired
    GameRepository repository;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanAndIndex() {
        mongoTemplate.dropCollection(GameDocument.class);
        mongoTemplate.indexOps(GameDocument.class).ensureIndex(
                new Index().on("game_id", Sort.Direction.ASC).unique().named("uniq_game_id"));
    }

    private static Game game(String id, LocalDate date) {
        return new Game(
                new GameId(id),
                date,
                KboTeam.HH,
                KboTeam.LG,
                GameStatus.FINAL,
                new Score(4, 3));
    }

    @Test
    void saveAndFindByGameId() {
        repository.save(GameMapper.toDocument(game("20260512HHLG", LocalDate.of(2026, 5, 12))));
        var found = repository.findByGameId("20260512HHLG");
        assertThat(found).isPresent();
        assertThat(found.get().homeTeam()).isEqualTo("HH");
    }

    @Test
    void uniqueGameIdIsEnforced() {
        repository.save(GameMapper.toDocument(game("20260512HHLG", LocalDate.of(2026, 5, 12))));
        assertThatThrownBy(() ->
                repository.save(GameMapper.toDocument(game("20260512HHLG", LocalDate.of(2026, 5, 12)))))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void findByDateBetweenRangeInclusive() {
        repository.save(GameMapper.toDocument(game("a", LocalDate.of(2026, 5, 12))));
        repository.save(GameMapper.toDocument(game("b", LocalDate.of(2026, 5, 15))));
        repository.save(GameMapper.toDocument(game("c", LocalDate.of(2026, 5, 20))));

        var inRange = repository.findByDateBetween(LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15));
        assertThat(inRange).extracting(GameDocument::gameId).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void ingestServiceUpsertReplacesByGameId() {
        var service = new GameIngestService(repository);
        service.upsert(new Game(
                new GameId("20260512HHLG"),
                LocalDate.of(2026, 5, 12),
                KboTeam.HH, KboTeam.LG,
                GameStatus.SCHEDULED,
                Score.zero()));
        service.upsert(new Game(
                new GameId("20260512HHLG"),
                LocalDate.of(2026, 5, 12),
                KboTeam.HH, KboTeam.LG,
                GameStatus.FINAL,
                new Score(4, 3)));

        var docs = repository.findAll();
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).status()).isEqualTo("FINAL");
        assertThat(docs.get(0).score().home()).isEqualTo(4);
    }
}
