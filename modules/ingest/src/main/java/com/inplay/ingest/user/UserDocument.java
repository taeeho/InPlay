package com.inplay.ingest.user;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB user collection — PLAN.md §3 schema.
 *
 * <p>인덱스 (mongo-init/02-init-collections.js): {@code uniq_user_id}, {@code uniq_api_key_hash}.
 */
@Document(collection = "user")
public record UserDocument(
        @Id String id,
        @Field("user_id") String userId,
        @Field("name") String name,
        @Field("api_key_hash") String apiKeyHash,
        @Field("my_team") String myTeam,
        @Field("rivalry_weights") Map<String, Double> rivalryWeights,
        @Field("discord_webhook_url") String discordWebhookUrl,
        @Field("mute_window") MuteWindowDoc muteWindow,
        @Field("created_at") Instant createdAt
) {

    public record MuteWindowDoc(String start, String end, String timezone) {}

    public UserDocument withId(String id) {
        return new UserDocument(id, userId, name, apiKeyHash, myTeam,
                rivalryWeights, discordWebhookUrl, muteWindow, createdAt);
    }
}
