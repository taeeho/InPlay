package com.inplay.api.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inplay.collector.kbo.ScheduleSource;
import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.ingest.game.GameIngestService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleCollectorServiceTest {

    @Mock ScheduleSource source;
    @Mock GameIngestService ingestService;

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 20);
    private static final int WINDOW = 7;

    private ScheduleCollectorService newService() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        return new ScheduleCollectorService(source, ingestService, WINDOW, clock);
    }

    private static Game game(String id, KboTeam home, KboTeam away) {
        return new Game(new GameId(id), TODAY, home, away, GameStatus.SCHEDULED, new Score(0, 0));
    }

    @Test
    void fetchesWindowAroundTodayAndUpserts() {
        ScheduleCollectorService svc = newService();
        List<Game> games = List.of(game("20260520HHLG", KboTeam.HH, KboTeam.LG));
        when(source.fetchSchedule(TODAY.minusDays(WINDOW), TODAY.plusDays(WINDOW))).thenReturn(games);
        when(ingestService.upsertAll(games)).thenReturn(1);

        int upserted = svc.collectToday();

        assertThat(upserted).isEqualTo(1);
        verify(ingestService).upsertAll(games);
    }

    @Test
    void skipsUpsertWhenNoGames() {
        ScheduleCollectorService svc = newService();
        when(source.fetchSchedule(any(), any())).thenReturn(List.of());

        int upserted = svc.collectToday();

        assertThat(upserted).isZero();
        verify(ingestService, never()).upsertAll(any());
    }

    @Test
    void propagatesUpsertCount() {
        ScheduleCollectorService svc = newService();
        List<Game> games = List.of(
                game("20260520HHLG", KboTeam.HH, KboTeam.LG),
                game("20260520KIASSG", KboTeam.KIA, KboTeam.SSG));
        when(source.fetchSchedule(eq(TODAY.minusDays(WINDOW)), eq(TODAY.plusDays(WINDOW)))).thenReturn(games);
        when(ingestService.upsertAll(games)).thenReturn(2);

        assertThat(svc.collectToday()).isEqualTo(2);
    }
}
