package com.inplay.ingest.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiveEventIngestServiceTest {

    @Mock LiveEventRepository repository;

    private LiveEvent event(String sourceEventId) {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:34:21Z"),
                new GameId("g1"),
                5,
                InningHalf.BOTTOM,
                LiveEventType.PITCH,
                1,
                new boolean[]{false, true, false},
                new Score(2, 2),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OptionalDouble.empty(),
                "naver_live",
                sourceEventId);
    }

    @Test
    void firstIngestSavesAndReturnsDocument() {
        var service = new LiveEventIngestService(repository);
        Mockito.when(repository.save(Mockito.any(LiveEventDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var saved = service.ingest(event("naver:1"));
        assertThat(saved).isPresent();
        assertThat(saved.get().sourceEventId()).isEqualTo("naver:1");
        Mockito.verify(repository).save(Mockito.any(LiveEventDocument.class));
    }

    @Test
    void duplicateWithinTtlIsSkipped() {
        var service = new LiveEventIngestService(repository);
        Mockito.when(repository.save(Mockito.any(LiveEventDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.ingest(event("naver:1"));
        var second = service.ingest(event("naver:1"));

        assertThat(second).isEmpty();
        Mockito.verify(repository, Mockito.times(1)).save(Mockito.any(LiveEventDocument.class));
    }

    @Test
    void differentSourceEventIdsBothIngested() {
        var service = new LiveEventIngestService(repository);
        Mockito.when(repository.save(Mockito.any(LiveEventDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.ingest(event("naver:1"))).isPresent();
        assertThat(service.ingest(event("naver:2"))).isPresent();
        Mockito.verify(repository, Mockito.times(2)).save(Mockito.any(LiveEventDocument.class));
    }

    @Test
    void cacheSizeTracksDistinctIngests() {
        var service = new LiveEventIngestService(repository);
        Mockito.when(repository.save(Mockito.any(LiveEventDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.ingest(event("naver:1"));
        service.ingest(event("naver:1"));
        service.ingest(event("naver:2"));

        assertThat(service.approximateCacheSize()).isEqualTo(2);
    }
}
