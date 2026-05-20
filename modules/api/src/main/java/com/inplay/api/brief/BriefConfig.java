package com.inplay.api.brief;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DefaultUserProperties.class)
public class BriefConfig {

    @Bean
    public Clock briefClock(DefaultUserProperties user) {
        return Clock.system(user.timezone() == null ? ZoneId.of("Asia/Seoul") : ZoneId.of(user.timezone()));
    }
}
