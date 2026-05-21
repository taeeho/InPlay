package com.inplay.ingest.user;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserDocument, String> {

    Optional<UserDocument> findByUserId(String userId);

    Optional<UserDocument> findByApiKeyHash(String apiKeyHash);
}
