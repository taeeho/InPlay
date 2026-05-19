package com.inplay.collector.kbo;

import com.inplay.collector.headless.PageRenderer;
import com.inplay.collector.headless.PlaywrightPageRenderer;
import com.inplay.collector.ratelimit.PollingRateLimiter;
import com.inplay.collector.robots.CachedRobotsGuard;
import com.inplay.collector.robots.RobotsGuard;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KboCollectorProperties.class)
public class KboCollectorConfig {

    @Bean
    @ConditionalOnMissingBean
    public KboScheduleParser kboScheduleParser() {
        return new KboScheduleParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public RobotsGuard robotsGuard(RestClient collectorRestClient) {
        return new CachedRobotsGuard(collectorRestClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PollingRateLimiter pollingRateLimiter() {
        return new PollingRateLimiter(Clock.systemDefaultZone());
    }

    @Bean
    @ConditionalOnProperty(prefix = "inplay.collector.kbo", name = "mode", havingValue = "http", matchIfMissing = true)
    public ScheduleSource httpScheduleSource(
            RestClient collectorRestClient,
            KboCollectorProperties properties,
            KboScheduleParser parser,
            RobotsGuard robotsGuard,
            PollingRateLimiter rateLimiter) {
        KboScheduleClient client = new KboScheduleClient(collectorRestClient, properties.scheduleUrl());
        return new KboHttpScheduleSource(client, parser, robotsGuard, rateLimiter);
    }

    @Bean
    @ConditionalOnProperty(prefix = "inplay.collector.kbo", name = "mode", havingValue = "headless")
    public PageRenderer playwrightPageRenderer() {
        return new PlaywrightPageRenderer();
    }

    @Bean
    @ConditionalOnProperty(prefix = "inplay.collector.kbo", name = "mode", havingValue = "headless")
    public ScheduleSource headlessScheduleSource(
            PageRenderer renderer,
            KboCollectorProperties properties,
            KboScheduleParser parser,
            RobotsGuard robotsGuard,
            PollingRateLimiter rateLimiter) {
        return new KboHeadlessScheduleSource(
                renderer, properties.scheduleUrl(), parser, robotsGuard, rateLimiter);
    }
}
