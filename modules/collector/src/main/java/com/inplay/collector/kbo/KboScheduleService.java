package com.inplay.collector.kbo;

import com.inplay.collector.http.CollectorUserAgent;
import com.inplay.collector.ratelimit.PollingMode;
import com.inplay.collector.ratelimit.PollingRateLimiter;
import com.inplay.collector.robots.RobotsGuard;
import com.inplay.core.domain.game.Game;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class KboScheduleService {

    private static final String RATE_LIMIT_KEY = "kbo:schedule";

    private final KboScheduleClient client;
    private final KboScheduleParser parser;
    private final RobotsGuard robotsGuard;
    private final PollingRateLimiter rateLimiter;

    public KboScheduleService(
            KboScheduleClient client,
            KboScheduleParser parser,
            RobotsGuard robotsGuard,
            PollingRateLimiter rateLimiter) {
        this.client = Objects.requireNonNull(client, "client required");
        this.parser = Objects.requireNonNull(parser, "parser required");
        this.robotsGuard = Objects.requireNonNull(robotsGuard, "robotsGuard required");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter required");
    }

    public List<Game> fetchSchedule(LocalDate from, LocalDate to) {
        var url = client.buildUrl(from, to);
        robotsGuard.enforce(url, CollectorUserAgent.VALUE);
        if (!rateLimiter.tryAcquire(RATE_LIMIT_KEY, PollingMode.NORMAL)) {
            return List.of();
        }
        String html = client.fetchScheduleHtml(from, to);
        return parser.parse(html);
    }
}
