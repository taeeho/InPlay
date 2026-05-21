package com.inplay.ingest.event;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.event.PitchInfo;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.id.PlayerId;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public final class LiveEventMapper {

    private LiveEventMapper() {}

    public static LiveEventDocument toDocument(LiveEvent e) {
        Objects.requireNonNull(e, "event required");
        var meta = new LiveEventDocument.Meta(e.gameId().value(), e.inning(), e.half().name());
        var pitch = e.pitch()
                .map(p -> new LiveEventDocument.PitchDoc(p.type(), p.speedKmh(), p.result()))
                .orElse(null);
        return new LiveEventDocument(
                null,
                e.eventTs(),
                meta,
                e.eventType().name(),
                e.outs(),
                e.runners(),
                new LiveEventDocument.ScoreDoc(e.score().home(), e.score().away()),
                e.batterId().map(PlayerId::value).orElse(null),
                e.pitcherId().map(PlayerId::value).orElse(null),
                pitch,
                e.wpaAfter().isPresent() ? e.wpaAfter().getAsDouble() : null,
                e.source(),
                e.sourceEventId());
    }

    public static LiveEvent toDomain(LiveEventDocument d) {
        Objects.requireNonNull(d, "doc required");
        var meta = d.meta();
        var pitch = d.pitch() == null
                ? Optional.<PitchInfo>empty()
                : Optional.of(new PitchInfo(d.pitch().type(), d.pitch().speedKmh(), d.pitch().result()));
        return new LiveEvent(
                d.eventTs(),
                new GameId(meta.gameId()),
                meta.inning(),
                InningHalf.valueOf(meta.half()),
                LiveEventType.valueOf(d.eventType()),
                d.outs(),
                d.runners(),
                new Score(d.score().home(), d.score().away()),
                d.batterId() == null ? Optional.empty() : Optional.of(new PlayerId(d.batterId())),
                d.pitcherId() == null ? Optional.empty() : Optional.of(new PlayerId(d.pitcherId())),
                pitch,
                d.wpaAfter() == null ? OptionalDouble.empty() : OptionalDouble.of(d.wpaAfter()),
                d.source(),
                d.sourceEventId());
    }
}
