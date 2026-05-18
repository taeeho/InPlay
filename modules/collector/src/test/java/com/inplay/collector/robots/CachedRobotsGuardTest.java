package com.inplay.collector.robots;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CachedRobotsGuardTest {

    private static final String UA = "inplay/0.1";

    private record Fixture(RestClient client, MockRestServiceServer server) {}

    private Fixture newFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(builder.build(), server);
    }

    @Test
    void allowsWhenRobotsAllows() {
        Fixture f = newFixture();
        f.server().expect(requestTo("http://kbo.example/robots.txt"))
                .andRespond(withSuccess("User-agent: *\nDisallow: /private\n", MediaType.TEXT_PLAIN));

        var guard = new CachedRobotsGuard(f.client());
        assertThat(guard.allowed(URI.create("http://kbo.example/schedule"), UA)).isTrue();
        f.server().verify();
    }

    @Test
    void blocksWhenDisallowMatches() {
        Fixture f = newFixture();
        f.server().expect(requestTo("http://kbo.example/robots.txt"))
                .andRespond(withSuccess("User-agent: *\nDisallow: /private\n", MediaType.TEXT_PLAIN));

        var guard = new CachedRobotsGuard(f.client());
        assertThat(guard.allowed(URI.create("http://kbo.example/private/a"), UA)).isFalse();
    }

    @Test
    void enforceThrowsRobotsViolation() {
        Fixture f = newFixture();
        f.server().expect(requestTo("http://kbo.example/robots.txt"))
                .andRespond(withSuccess("User-agent: *\nDisallow: /\n", MediaType.TEXT_PLAIN));

        var guard = new CachedRobotsGuard(f.client());
        assertThatThrownBy(() -> guard.enforce(URI.create("http://kbo.example/x"), UA))
                .isInstanceOf(RobotsViolationException.class);
    }

    @Test
    void missingRobotsTreatedAsAllowAll() {
        Fixture f = newFixture();
        f.server().expect(requestTo("http://kbo.example/robots.txt"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        var guard = new CachedRobotsGuard(f.client());
        assertThat(guard.allowed(URI.create("http://kbo.example/anything"), UA)).isTrue();
    }

    @Test
    void cacheReusesFetchedRule() {
        Fixture f = newFixture();
        f.server().expect(ExpectedCount.once(), requestTo("http://kbo.example/robots.txt"))
                .andRespond(withSuccess("User-agent: *\nDisallow: /private\n", MediaType.TEXT_PLAIN));

        var guard = new CachedRobotsGuard(f.client());
        guard.allowed(URI.create("http://kbo.example/a"), UA);
        guard.allowed(URI.create("http://kbo.example/b"), UA);
        guard.allowed(URI.create("http://kbo.example/private/x"), UA);
        f.server().verify();
    }
}
