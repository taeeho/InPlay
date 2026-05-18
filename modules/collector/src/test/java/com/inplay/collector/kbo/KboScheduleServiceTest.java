package com.inplay.collector.kbo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.collector.ratelimit.PollingMode;
import com.inplay.collector.ratelimit.PollingRateLimiter;
import com.inplay.collector.robots.RobotsGuard;
import com.inplay.collector.robots.RobotsViolationException;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KboScheduleServiceTest {

    private static final URI SCHEDULE_URL = URI.create("http://kbo.example/schedule");
    private static final LocalDate FROM = LocalDate.of(2026, 5, 12);
    private static final LocalDate TO = LocalDate.of(2026, 5, 18);

    private static final String FIXTURE_HTML = """
            <table class="schedule">
              <tr class="game" data-game-id="20260512HHLG" data-date="2026-05-12">
                <td class="home">HH</td><td class="score">4-3</td><td class="away">LG</td><td class="status">FINAL</td>
              </tr>
            </table>
            """;

    private record Fixture(RestClient client, MockRestServiceServer server) {}

    private Fixture newFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(builder.build(), server);
    }

    private KboScheduleService newService(RestClient http, RobotsGuard robots, PollingRateLimiter limiter) {
        return new KboScheduleService(
                new KboScheduleClient(http, SCHEDULE_URL),
                new KboScheduleParser(),
                robots,
                limiter);
    }

    @Test
    void fetchesParsesAndReturnsGames() {
        Fixture f = newFixture();
        f.server().expect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo("http://kbo.example/schedule?from=2026-05-12&to=2026-05-18"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess(FIXTURE_HTML, org.springframework.http.MediaType.TEXT_HTML));

        var service = newService(
                f.client(),
                (url, ua) -> true,
                new PollingRateLimiter(Clock.fixed(LocalDate.of(2026, 5, 12).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)));

        List<com.inplay.core.domain.game.Game> games = service.fetchSchedule(FROM, TO);
        assertThat(games).hasSize(1);
        assertThat(games.get(0).id().value()).isEqualTo("20260512HHLG");
        f.server().verify();
    }

    @Test
    void robotsViolationStopsFetch() {
        Fixture f = newFixture();
        var service = newService(
                f.client(),
                (url, ua) -> false,
                new PollingRateLimiter(Clock.systemUTC()));

        assertThatThrownBy(() -> service.fetchSchedule(FROM, TO))
                .isInstanceOf(RobotsViolationException.class);
        f.server().verify(); // no HTTP calls
    }

    @Test
    void rateLimitedFetchReturnsEmpty() {
        Fixture f = newFixture();
        AtomicReference<java.time.Instant> nowRef = new AtomicReference<>(java.time.Instant.parse("2026-05-12T10:00:00Z"));
        Clock clock = new Clock() {
            @Override public java.time.Instant instant() { return nowRef.get(); }
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        };
        var limiter = new PollingRateLimiter(clock);

        f.server().expect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo("http://kbo.example/schedule?from=2026-05-12&to=2026-05-18"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess(FIXTURE_HTML, org.springframework.http.MediaType.TEXT_HTML));

        var service = newService(f.client(), (url, ua) -> true, limiter);

        assertThat(service.fetchSchedule(FROM, TO)).hasSize(1);
        nowRef.set(nowRef.get().plus(java.time.Duration.ofSeconds(30)));
        assertThat(service.fetchSchedule(FROM, TO)).isEmpty();
        f.server().verify();
    }

    @Test
    void buildUrlRejectsInvertedRange() {
        var client = new KboScheduleClient(RestClient.builder().build(), SCHEDULE_URL);
        assertThatThrownBy(() -> client.buildUrl(TO, FROM))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
