package com.inplay.api.brief;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 08:00 KST에 default user 대상 brief 발송 트리거.
 * inplay.brief.scheduler.enabled=false 로 테스트·CLI 환경에서 비활성.
 */
@Component
@ConditionalOnProperty(prefix = "inplay.brief.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DailyBriefScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyBriefScheduler.class);

    private final DailyBriefService service;

    public DailyBriefScheduler(DailyBriefService service) {
        this.service = service;
    }

    @Scheduled(cron = "${inplay.brief.cron:0 0 8 * * *}", zone = "${inplay.brief.timezone:Asia/Seoul}")
    public void run() {
        try {
            int sent = service.sendBriefsForToday();
            log.info("daily brief tick sent={}", sent);
        } catch (RuntimeException e) {
            log.error("daily brief tick failed", e);
        }
    }
}
