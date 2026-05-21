package com.inplay.ingest.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.core.domain.id.UserId;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.core.domain.user.User;
import com.inplay.core.domain.user.UserMuteWindow;

import java.time.Instant;
import java.util.Map;
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
class UserRepositoryIT {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }

    @Autowired UserRepository repository;
    @Autowired MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanAndIndex() {
        mongoTemplate.dropCollection(UserDocument.class);
        mongoTemplate.indexOps(UserDocument.class).ensureIndex(
                new Index().on("user_id", Sort.Direction.ASC).unique().named("uniq_user_id"));
        mongoTemplate.indexOps(UserDocument.class).ensureIndex(
                new Index().on("api_key_hash", Sort.Direction.ASC).unique().named("uniq_api_key_hash"));
    }

    private static User user(String userId, String hash) {
        return new User(
                new UserId(userId), userId, hash, KboTeam.HH,
                Map.of(KboTeam.LG, 1.3),
                null, UserMuteWindow.disabled(),
                Instant.parse("2026-05-12T00:00:00Z"));
    }

    @Test
    void saveAndFindByUserId() {
        repository.save(UserMapper.toDocument(user("u_taeeho", "a".repeat(64))));
        var found = repository.findByUserId("u_taeeho");
        assertThat(found).isPresent();
        assertThat(found.get().myTeam()).isEqualTo("HH");
    }

    @Test
    void findByApiKeyHashWorks() {
        String hash = "c".repeat(64);
        repository.save(UserMapper.toDocument(user("u_friend", hash)));
        var found = repository.findByApiKeyHash(hash);
        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo("u_friend");
    }

    @Test
    void uniqueUserIdEnforced() {
        repository.save(UserMapper.toDocument(user("u_taeeho", "a".repeat(64))));
        assertThatThrownBy(() ->
                repository.save(UserMapper.toDocument(user("u_taeeho", "b".repeat(64)))))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void uniqueApiKeyHashEnforced() {
        String hash = "d".repeat(64);
        repository.save(UserMapper.toDocument(user("u_a", hash)));
        assertThatThrownBy(() ->
                repository.save(UserMapper.toDocument(user("u_b", hash))))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
