package com.inplay.ingest.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.event.PitchInfo;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.id.PlayerId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.TimeSeriesOptions;
import org.springframework.data.mongodb.core.timeseries.Granularity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Verifies LiveEvent ingest against a real MongoDB 7 timeseries collection (Testcontainers).
@Tag("integration")
@Testcontainers
@DataMongoTest
class LiveEventRepositoryIT {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }

    @Autowired LiveEventRepository repository;
    @Autowired MongoTemplate mongoTemplate;

    @BeforeEach
    void recreateTimeseries() {
        if (mongoTemplate.collectionExists(LiveEventDocument.class)) {
            mongoTemplate.dropCollection(LiveEventDocument.class);
        }
        var options = CollectionOptions.empty()
                .timeSeries(TimeSeriesOptions.timeSeries("event_ts")
                        .metaField("meta")
                        .granularity(Granularity.SECONDS));
        mongoTemplate.createCollection(LiveEventDocument.class, options);
    }

    private LiveEvent event(String gameId, String sourceEventId, Instant ts) {
        return new LiveEvent(
                ts,
                new GameId(gameId),
                5,
                InningHalf.BOTTOM,
                LiveEventType.PITCH,
                2,
                new boolean[]{true, true, true},
                new Score(3, 4),
                Optional.of(new PlayerId("p_b")),
                Optional.of(new PlayerId("p_p")),
                Optional.of(new PitchInfo("FF", 147, "ball")),
                OptionalDouble.empty(),
                "naver_live",
                sourceEventId);
    }

    @Test
    void saveAndQueryByGameId() {
        var service = new LiveEventIngestService(repository);
        service.ingest(event("g1", "e1", Instant.parse("2026-05-12T19:34:21Z")));
        service.ingest(event("g1", "e2", Instant.parse("2026-05-12T19:34:30Z")));
        service.ingest(event("g2", "e3", Instant.parse("2026-05-12T19:34:31Z")));

        List<LiveEventDocument> g1 = repository.findByGameId("g1");
        assertThat(g1).hasSize(2);
        assertThat(g1).extracting(d -> d.meta().gameId()).containsOnly("g1");
        assertThat(repository.countByGameId("g1")).isEqualTo(2);
    }

    @Test
    void caffeineDedupePreventsDoubleInsert() {
        var service = new LiveEventIngestService(repository);
        service.ingest(event("g1", "dup", Instant.parse("2026-05-12T19:34:21Z")));
        var second = service.ingest(event("g1", "dup", Instant.parse("2026-05-12T19:34:30Z")));
        assertThat(second).isEmpty();
        assertThat(repository.countByGameId("g1")).isEqualTo(1);
    }

    @Test
    void collectionRegisteredAsTimeseries() {
        Document info = mongoTemplate.getDb()
                .runCommand(new Document("listCollections", 1)
                        .append("filter", new Document("name", "live_event")));
        @SuppressWarnings("unchecked")
        var collections = (List<Document>) ((Document) info.get("cursor")).get("firstBatch");
        assertThat(collections).hasSize(1);
        assertThat(collections.get(0).getString("type")).isEqualTo("timeseries");
    }
}
