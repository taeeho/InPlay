package com.inplay.api.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 밤(기본 23:30 KST) 당일 종료 경기를 Notion 시즌 일지에 기록.
 * inplay.journal.scheduler.enabled=false 로 테스트·CLI 환경에서 비활성.
 */
@Component
@ConditionalOnProperty(prefix = "inplay.journal.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeasonJournalScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeasonJournalScheduler.class);

    private final SeasonJournalService service;

    public SeasonJournalScheduler(SeasonJournalService service) {
        this.service = service;
    }

    @Scheduled(cron = "${inplay.journal.cron:0 30 23 * * *}", zone = "${inplay.journal.timezone:Asia/Seoul}")
    public void run() {
        try {
            int written = service.writeJournalsForToday();
            log.info("season journal tick written={}", written);
        } catch (RuntimeException e) {
            log.error("season journal tick failed", e);
        }
    }
}
