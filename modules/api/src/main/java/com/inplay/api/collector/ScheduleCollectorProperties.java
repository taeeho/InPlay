package com.inplay.api.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 일정 수집 폴링 설정. enabled/interval 은 스케줄러 애너테이션 placeholder 로 직접 참조되고,
 * 여기서는 fetch 윈도우(오늘 기준 ± windowDays)만 바인딩한다.
 */
@ConfigurationProperties(prefix = "inplay.collector.schedule")
public record ScheduleCollectorProperties(Integer windowDays) {

    public ScheduleCollectorProperties {
        if (windowDays == null || windowDays < 1) {
            windowDays = 7;
        }
    }
}
