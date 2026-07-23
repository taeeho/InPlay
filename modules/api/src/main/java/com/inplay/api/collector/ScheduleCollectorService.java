package com.inplay.api.collector;

import com.inplay.collector.kbo.ScheduleSource;
import com.inplay.core.domain.game.Game;
import com.inplay.ingest.game.GameIngestService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KBO 일정을 수집해 mongo {@code game} 컬렉션에 upsert. 오늘 기준 ± windowDays 구간을 요청한다.
 * 실제 수집은 {@link ScheduleSource} 구현(http/headless, mode 로 선택)에 위임.
 */
public class ScheduleCollectorService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleCollectorService.class);

    private final ScheduleSource source;
    private final GameIngestService ingestService;
    private final int windowDays;
    private final Clock clock;

    public ScheduleCollectorService(
            ScheduleSource source,
            GameIngestService ingestService,
            int windowDays,
            Clock clock) {
        this.source = Objects.requireNonNull(source, "source required");
        this.ingestService = Objects.requireNonNull(ingestService, "ingestService required");
        this.windowDays = windowDays;
        this.clock = Objects.requireNonNull(clock, "clock required");
    }

    /** @return upsert 한 경기 수. */
    public int collectToday() {
        LocalDate today = LocalDate.now(clock);
        return collect(today.minusDays(windowDays), today.plusDays(windowDays));
    }

    int collect(LocalDate from, LocalDate to) {
        List<Game> games = source.fetchSchedule(from, to);
        if (games.isEmpty()) {
            log.info("schedule collect window={}..{} returned no games", from, to);
            return 0;
        }
        int upserted = ingestService.upsertAll(games);
        log.info("schedule collect upserted={} window={}..{}", upserted, from, to);
        return upserted;
    }
}
