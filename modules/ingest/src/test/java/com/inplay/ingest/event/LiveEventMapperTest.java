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
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class LiveEventMapperTest {

    private LiveEvent rich() {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:34:21Z"),
                new GameId("20260512HHLG"),
                5,
                InningHalf.BOTTOM,
                LiveEventType.PITCH,
                2,
                new boolean[]{true, true, true},
                new Score(3, 4),
                Optional.of(new PlayerId("p_batter")),
                Optional.of(new PlayerId("p_pitcher")),
                Optional.of(new PitchInfo("FF", 147, "ball")),
                OptionalDouble.of(0.412),
                "naver_live",
                "naver:abc123");
    }

    private LiveEvent sparse() {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:35:00Z"),
                new GameId("20260512HHLG"),
                5,
                InningHalf.BOTTOM,
                LiveEventType.END_INNING,
                3,
                new boolean[3],
                new Score(3, 4),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OptionalDouble.empty(),
                "kbo_official",
                "kbo:xyz");
    }

    @Test
    void richEventRoundTrips() {
        var doc = LiveEventMapper.toDocument(rich());
        var back = LiveEventMapper.toDomain(doc);
        assertThat(back.gameId().value()).isEqualTo("20260512HHLG");
        assertThat(back.inning()).isEqualTo(5);
        assertThat(back.half()).isEqualTo(InningHalf.BOTTOM);
        assertThat(back.eventType()).isEqualTo(LiveEventType.PITCH);
        assertThat(back.pitch()).isPresent();
        assertThat(back.pitch().get().speedKmh()).isEqualTo(147);
        assertThat(back.wpaAfter()).hasValue(0.412);
        assertThat(back.batterId()).map(PlayerId::value).hasValue("p_batter");
        assertThat(back.runnerOn(1)).isTrue();
        assertThat(back.sourceEventId()).isEqualTo("naver:abc123");
    }

    @Test
    void sparseEventOmitsOptionalFields() {
        var doc = LiveEventMapper.toDocument(sparse());
        assertThat(doc.pitch()).isNull();
        assertThat(doc.batterId()).isNull();
        assertThat(doc.pitcherId()).isNull();
        assertThat(doc.wpaAfter()).isNull();

        var back = LiveEventMapper.toDomain(doc);
        assertThat(back.pitch()).isEmpty();
        assertThat(back.batterId()).isEmpty();
        assertThat(back.wpaAfter()).isEmpty();
    }

    @Test
    void metaSubdocumentCarriesGameIdInningHalf() {
        var doc = LiveEventMapper.toDocument(rich());
        assertThat(doc.meta().gameId()).isEqualTo("20260512HHLG");
        assertThat(doc.meta().inning()).isEqualTo(5);
        assertThat(doc.meta().half()).isEqualTo("BOTTOM");
    }
}
