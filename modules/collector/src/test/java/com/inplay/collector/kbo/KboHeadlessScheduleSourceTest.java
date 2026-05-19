package com.inplay.collector.kbo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.collector.headless.PageRenderer;
import com.inplay.collector.ratelimit.PollingRateLimiter;
import com.inplay.collector.robots.RobotsGuard;
import com.inplay.collector.robots.RobotsViolationException;
import com.inplay.core.domain.game.Game;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class KboHeadlessScheduleSourceTest {

    private static final URI SCHEDULE_URL =
            URI.create("https://www.koreabaseball.com/Schedule/Schedule.aspx");
    private static final LocalDate FROM = LocalDate.of(2026, 5, 12);
    private static final LocalDate TO = LocalDate.of(2026, 5, 18);

    private static final String FIXTURE_HTML = """
            <html><body>
            <table id="tblScheduleList" class="schedule">
              <tr class="game" data-game-id="20260512HHLG" data-date="2026-05-12">
                <td class="home">HH</td><td class="score">4-3</td><td class="away">LG</td><td class="status">FINAL</td>
              </tr>
            </table>
            </body></html>
            """;

    private static PollingRateLimiter fixedRateLimiter() {
        return new PollingRateLimiter(
                Clock.fixed(Instant.parse("2026-05-12T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void rendererInvokedWithAdr009Options() {
        AtomicReference<URI> capturedUrl = new AtomicReference<>();
        AtomicReference<PageRenderer.RenderOptions> capturedOpts = new AtomicReference<>();
        PageRenderer renderer = (url, opts) -> {
            capturedUrl.set(url);
            capturedOpts.set(opts);
            return FIXTURE_HTML;
        };

        var source = new KboHeadlessScheduleSource(
                renderer, SCHEDULE_URL, new KboScheduleParser(), (u, ua) -> true, fixedRateLimiter());

        List<Game> games = source.fetchSchedule(FROM, TO);
        assertThat(games).hasSize(1);
        assertThat(games.get(0).id().value()).isEqualTo("20260512HHLG");

        assertThat(capturedUrl.get()).isEqualTo(SCHEDULE_URL);
        assertThat(capturedOpts.get().userAgent())
                .contains("inplay-headless/0.1")
                .contains("ai@ccfm.co.kr");
        assertThat(capturedOpts.get().abortPatterns()).contains("**/ws/**");
        assertThat(capturedOpts.get().waitForSelector()).isEqualTo("#tblScheduleList tr");
        assertThat(capturedOpts.get().timeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void robotsViolationStopsRender() {
        AtomicReference<Boolean> rendered = new AtomicReference<>(false);
        PageRenderer renderer = (url, opts) -> {
            rendered.set(true);
            return FIXTURE_HTML;
        };

        var source = new KboHeadlessScheduleSource(
                renderer, SCHEDULE_URL, new KboScheduleParser(), (u, ua) -> false, fixedRateLimiter());

        assertThatThrownBy(() -> source.fetchSchedule(FROM, TO))
                .isInstanceOf(RobotsViolationException.class);
        assertThat(rendered.get()).isFalse();
    }

    @Test
    void rateLimitedReturnsEmpty() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-05-12T10:00:00Z"));
        Clock clock = new Clock() {
            @Override public Instant instant() { return now.get(); }
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
        };
        var limiter = new PollingRateLimiter(clock);
        PageRenderer renderer = (url, opts) -> FIXTURE_HTML;
        var source = new KboHeadlessScheduleSource(
                renderer, SCHEDULE_URL, new KboScheduleParser(), (u, ua) -> true, limiter);

        assertThat(source.fetchSchedule(FROM, TO)).hasSize(1);
        now.set(now.get().plus(Duration.ofSeconds(30)));
        assertThat(source.fetchSchedule(FROM, TO)).isEmpty();
    }

    @Test
    void invertedRangeRejected() {
        PageRenderer renderer = (url, opts) -> FIXTURE_HTML;
        var source = new KboHeadlessScheduleSource(
                renderer, SCHEDULE_URL, new KboScheduleParser(), (u, ua) -> true, fixedRateLimiter());
        assertThatThrownBy(() -> source.fetchSchedule(TO, FROM))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
