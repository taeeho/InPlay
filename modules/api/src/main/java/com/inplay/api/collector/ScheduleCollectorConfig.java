package com.inplay.api.collector;

import com.inplay.collector.kbo.ScheduleSource;
import com.inplay.ingest.game.GameIngestService;
import com.inplay.ingest.game.GameRepository;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 일정 수집 wiring: ScheduleSource(collector) → GameIngestService(ingest) → mongo.
 * ScheduleSource 빈은 KboCollectorConfig 가 mode(http/headless) 에 따라 제공.
 */
@Configuration
@EnableConfigurationProperties(ScheduleCollectorProperties.class)
public class ScheduleCollectorConfig {

    @Bean
    public GameIngestService gameIngestService(GameRepository repository) {
        return new GameIngestService(repository);
    }

    @Bean
    public ScheduleCollectorService scheduleCollectorService(
            ScheduleSource scheduleSource,
            GameIngestService gameIngestService,
            ScheduleCollectorProperties properties,
            Clock clock) {
        return new ScheduleCollectorService(
                scheduleSource, gameIngestService, properties.windowDays(), clock);
    }
}
