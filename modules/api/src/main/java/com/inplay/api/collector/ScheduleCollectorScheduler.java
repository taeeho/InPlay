package com.inplay.api.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주기적으로 KBO 일정을 수집해 mongo 에 적재. 기본 5분 간격(부팅 후 10초 뒤 첫 tick).
 * inplay.collector.schedule.enabled=false 로 테스트·CLI 환경에서 비활성.
 */
@Component
@ConditionalOnProperty(prefix = "inplay.collector.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScheduleCollectorScheduler.class);

    private final ScheduleCollectorService service;

    public ScheduleCollectorScheduler(ScheduleCollectorService service) {
        this.service = service;
    }

    // fixedDelayString/initialDelayString 은 ISO-8601(PT…) 또는 millis 만 허용 — "5m" 같은 simple 표기 불가.
    @Scheduled(
            fixedDelayString = "${inplay.collector.schedule.interval:PT5M}",
            initialDelayString = "${inplay.collector.schedule.initial-delay:PT10S}")
    public void run() {
        try {
            int upserted = service.collectToday();
            log.info("schedule collect tick upserted={}", upserted);
        } catch (RuntimeException e) {
            log.error("schedule collect tick failed", e);
        }
    }
}
