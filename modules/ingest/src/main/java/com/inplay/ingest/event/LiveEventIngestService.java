package com.inplay.ingest.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.inplay.core.domain.event.LiveEvent;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

// Ingest live events with source_event_id dedupe.
//
// Why a Caffeine cache (not Mongo unique index):
//   timeseries collections in MongoDB 7 do NOT support unique indexes on body fields.
//   Polling sources (네이버 라이브 등) emit the same physical pitch under one source_event_id
//   every 30s tick. Application-side cache (60s TTL) is the primary dedupe layer.
//
// Idempotent: same source_event_id seen within TTL → no-op, returns empty Optional.
// Out-of-TTL duplicates (>60s) are accepted; downstream queries should de-dup on read if needed.
public class LiveEventIngestService {

    public static final Duration DEFAULT_DEDUPE_TTL = Duration.ofSeconds(60);

    private final LiveEventRepository repository;
    private final Cache<String, Boolean> seenEventIds;

    public LiveEventIngestService(LiveEventRepository repository) {
        this(repository, DEFAULT_DEDUPE_TTL);
    }

    public LiveEventIngestService(LiveEventRepository repository, Duration dedupeTtl) {
        this.repository = Objects.requireNonNull(repository, "repository required");
        Objects.requireNonNull(dedupeTtl, "dedupeTtl required");
        this.seenEventIds = Caffeine.newBuilder()
                .expireAfterWrite(dedupeTtl)
                .maximumSize(50_000)
                .build();
    }

    public Optional<LiveEventDocument> ingest(LiveEvent event) {
        Objects.requireNonNull(event, "event required");
        String key = event.sourceEventId();
        if (seenEventIds.getIfPresent(key) != null) {
            return Optional.empty();
        }
        seenEventIds.put(key, Boolean.TRUE);
        LiveEventDocument doc = LiveEventMapper.toDocument(event);
        return Optional.of(repository.save(doc));
    }

    public long approximateCacheSize() {
        return seenEventIds.estimatedSize();
    }
}
