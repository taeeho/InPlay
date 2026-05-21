package com.inplay.core.domain.event;

import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.id.PlayerId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

// LiveEvent — single live-game atomic event (PLAN.md §3 live_event schema).
//
// runners: 3-slot boolean array [first, second, third]. Matches PLAN's [1,1,1] notation.
// sourceEventId: dedupe key (60s Caffeine cache + idempotent ingest).
// wpaAfter: stamped post-ingest by the WPA annotator (decision/wpa) — empty on first ingest.
public record LiveEvent(
        Instant eventTs,
        GameId gameId,
        int inning,
        InningHalf half,
        LiveEventType eventType,
        int outs,
        boolean[] runners,
        Score score,
        Optional<PlayerId> batterId,
        Optional<PlayerId> pitcherId,
        Optional<PitchInfo> pitch,
        OptionalDouble wpaAfter,
        String source,
        String sourceEventId
) {
    public LiveEvent {
        Objects.requireNonNull(eventTs, "eventTs required");
        Objects.requireNonNull(gameId, "gameId required");
        Objects.requireNonNull(half, "half required");
        Objects.requireNonNull(eventType, "eventType required");
        Objects.requireNonNull(score, "score required");
        Objects.requireNonNull(batterId, "batterId required (use Optional.empty())");
        Objects.requireNonNull(pitcherId, "pitcherId required (use Optional.empty())");
        Objects.requireNonNull(pitch, "pitch required (use Optional.empty())");
        Objects.requireNonNull(wpaAfter, "wpaAfter required (use OptionalDouble.empty())");
        Objects.requireNonNull(source, "source required");
        Objects.requireNonNull(sourceEventId, "sourceEventId required");
        if (source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        if (sourceEventId.isBlank()) {
            throw new IllegalArgumentException("sourceEventId must not be blank");
        }
        if (inning < 1) {
            throw new IllegalArgumentException("inning must be >= 1, got " + inning);
        }
        if (outs < 0 || outs > 3) {
            throw new IllegalArgumentException("outs must be in 0..3, got " + outs);
        }
        Objects.requireNonNull(runners, "runners required");
        if (runners.length != 3) {
            throw new IllegalArgumentException("runners must have length 3, got " + runners.length);
        }
        runners = runners.clone();
    }

    @Override
    public boolean[] runners() {
        return runners.clone();
    }

    public boolean runnerOn(int base) {
        if (base < 1 || base > 3) {
            throw new IllegalArgumentException("base must be in 1..3, got " + base);
        }
        return runners[base - 1];
    }

    public LiveEvent withWpaAfter(double wpaAfter) {
        return new LiveEvent(
                eventTs, gameId, inning, half, eventType,
                outs, runners, score, batterId, pitcherId, pitch,
                OptionalDouble.of(wpaAfter),
                source, sourceEventId);
    }
}
