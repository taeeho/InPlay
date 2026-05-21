package com.inplay.ingest.event;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

// MongoDB representation of LiveEvent.
// Collection is timeseries (see infra/compose/mongo-init/02-init-collections.js):
//   timeField = event_ts, metaField = meta, granularity = seconds, TTL = 240 days.
// Mongo timeseries does NOT support unique indexes — dedupe is application-side
// via Caffeine cache on source_event_id (LiveEventIngestService).
@Document(collection = "live_event")
public record LiveEventDocument(
        @Id String id,
        @Field("event_ts") Instant eventTs,
        @Field("meta") Meta meta,
        @Field("event_type") String eventType,
        @Field("outs") int outs,
        @Field("runners") boolean[] runners,
        @Field("score") ScoreDoc score,
        @Field("batter_id") String batterId,
        @Field("pitcher_id") String pitcherId,
        @Field("pitch") PitchDoc pitch,
        @Field("wpa_after") Double wpaAfter,
        @Field("source") String source,
        @Field("source_event_id") String sourceEventId
) {

    public record Meta(
            @Field("game_id") String gameId,
            @Field("inning") int inning,
            @Field("half") String half
    ) {}

    public record ScoreDoc(int home, int away) {}

    public record PitchDoc(String type, int speedKmh, String result) {}
}
