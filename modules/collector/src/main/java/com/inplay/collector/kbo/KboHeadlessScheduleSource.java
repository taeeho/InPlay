package com.inplay.collector.kbo;

import com.inplay.collector.headless.PageRenderer;
import com.inplay.collector.ratelimit.PollingMode;
import com.inplay.collector.ratelimit.PollingRateLimiter;
import com.inplay.collector.robots.RobotsGuard;
import com.inplay.core.domain.game.Game;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * KBO 일정을 headless 브라우저로 SSR된 DOM만 파싱. ADR-009 운영 조건 코드화.
 * - 브라우저 UA 명시 (정체 노출)
 * - /ws/** AJAX route abort (robots disallow path 회피)
 * - polling RateLimiter (분당 1회)
 * - RobotsGuard로 navigate URL 검증
 */
public final class KboHeadlessScheduleSource implements ScheduleSource {

    public static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "inplay-headless/0.1 (+contact: ai@ccfm.co.kr)";

    private static final String RATE_LIMIT_KEY = "kbo:schedule:headless";
    private static final String SCHEDULE_TABLE_SELECTOR = "#tblScheduleList tr";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final List<String> ABORT_PATTERNS = List.of("**/ws/**");

    private final PageRenderer renderer;
    private final URI scheduleUrl;
    private final KboScheduleParser parser;
    private final RobotsGuard robotsGuard;
    private final PollingRateLimiter rateLimiter;

    public KboHeadlessScheduleSource(
            PageRenderer renderer,
            URI scheduleUrl,
            KboScheduleParser parser,
            RobotsGuard robotsGuard,
            PollingRateLimiter rateLimiter) {
        this.renderer = Objects.requireNonNull(renderer, "renderer required");
        this.scheduleUrl = Objects.requireNonNull(scheduleUrl, "scheduleUrl required");
        this.parser = Objects.requireNonNull(parser, "parser required");
        this.robotsGuard = Objects.requireNonNull(robotsGuard, "robotsGuard required");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter required");
    }

    @Override
    public List<Game> fetchSchedule(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from required");
        Objects.requireNonNull(to, "to required");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must not be before from: " + from + " > " + to);
        }
        robotsGuard.enforce(scheduleUrl, USER_AGENT);
        if (!rateLimiter.tryAcquire(RATE_LIMIT_KEY, PollingMode.NORMAL)) {
            return List.of();
        }
        PageRenderer.RenderOptions options = new PageRenderer.RenderOptions(
                USER_AGENT,
                ABORT_PATTERNS,
                DEFAULT_TIMEOUT,
                SCHEDULE_TABLE_SELECTOR);
        String html = renderer.render(scheduleUrl, options);
        return parser.parse(html);
    }
}
