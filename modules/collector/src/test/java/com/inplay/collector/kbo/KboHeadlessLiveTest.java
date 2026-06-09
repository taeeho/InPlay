package com.inplay.collector.kbo;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.collector.headless.PlaywrightPageRenderer;
import com.inplay.collector.ratelimit.PollingRateLimiter;
import com.inplay.core.domain.game.Game;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * 실 KBO {@code /Schedule/Schedule.aspx} 를 headless 렌더해 {@link KboScheduleParser} 동작을 검증.
 *
 * <p>기본 비활성 — {@code KBO_LIVE=1} 환경변수 + Playwright Chromium 설치 이미지에서만 실행
 * ({@code make test-headless}). ADR-009 폴링 1회만 수행하며 수집 데이터는 커밋하지 않는다.
 */
@EnabledIfEnvironmentVariable(named = "KBO_LIVE", matches = "1")
class KboHeadlessLiveTest {

    private static final URI SCHEDULE_URL =
            URI.create("https://www.koreabaseball.com/Schedule/Schedule.aspx");

    @Test
    void rendersRealScheduleAndParsesKnownTeams() {
        LocalDate today = LocalDate.now();
        try (PlaywrightPageRenderer renderer = new PlaywrightPageRenderer()) {
            KboHeadlessScheduleSource source = new KboHeadlessScheduleSource(
                    renderer,
                    SCHEDULE_URL,
                    new KboScheduleParser(),
                    (url, ua) -> true,
                    new PollingRateLimiter(Clock.systemUTC()));

            List<Game> games = source.fetchSchedule(today.withDayOfMonth(1), today);

            assertThat(games).isNotEmpty();
            assertThat(games).allSatisfy(g -> {
                assertThat(g.homeTeam()).isNotNull();
                assertThat(g.awayTeam()).isNotNull();
                assertThat(g.homeTeam()).isNotEqualTo(g.awayTeam());
                assertThat(g.date().getYear()).isEqualTo(today.getYear());
            });
        }
    }
}
