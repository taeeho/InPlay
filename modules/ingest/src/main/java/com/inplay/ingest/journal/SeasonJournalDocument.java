package com.inplay.ingest.journal;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * season_journal 적재 마커 — "이 (user, season, game)의 일지를 Notion에 생성했다"는 기록.
 *
 * <p>중복 발송 방지용. 인덱스 (mongo-init/02-init-collections.js): unique {@code uniq_user_season_game}
 * ({@code (user_id, season, game_id)}). Notion page 본문은 Notion에만, 여기엔 식별 키 + 생성 시각만 둔다.
 */
@Document(collection = "season_journal")
public record SeasonJournalDocument(
        @Id String id,
        @Field("user_id") String userId,
        @Field("season") int season,
        @Field("game_id") String gameId,
        @Field("created_at") Instant createdAt) {

    public static SeasonJournalDocument forNew(String userId, int season, String gameId, Instant createdAt) {
        return new SeasonJournalDocument(null, userId, season, gameId, createdAt);
    }
}
