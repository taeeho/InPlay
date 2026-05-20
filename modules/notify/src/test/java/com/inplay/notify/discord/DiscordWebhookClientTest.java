package com.inplay.notify.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DiscordWebhookClientTest {

    private static final URI HOOK = URI.create("https://discord.example/api/webhooks/123/abc");

    private record Fixture(DiscordWebhookClient client, MockRestServiceServer server) {}

    private Fixture newFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new DiscordWebhookClient(builder.build()), server);
    }

    @Test
    void postsContentAsJson() {
        Fixture f = newFixture();
        f.server().expect(requestTo(HOOK))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"content\":\"hello\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"username\":\"inplay\"")))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        boolean ok = f.client().send(HOOK, DiscordWebhookPayload.of("hello"));

        assertThat(ok).isTrue();
        f.server().verify();
    }

    @Test
    void returnsFalseOn4xxWithoutThrowing() {
        Fixture f = newFixture();
        f.server().expect(requestTo(HOOK))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"retry_after\":3.0}"));

        boolean ok = f.client().send(HOOK, DiscordWebhookPayload.of("rate-limited"));

        assertThat(ok).isFalse();
    }

    @Test
    void returnsFalseOn5xx() {
        Fixture f = newFixture();
        f.server().expect(requestTo(HOOK))
                .andRespond(withRawStatus(500).body(""));

        boolean ok = f.client().send(HOOK, DiscordWebhookPayload.of("server-down"));

        assertThat(ok).isFalse();
    }

    @Test
    void returnsFalseOnNetworkError() {
        Fixture f = newFixture();
        f.server().expect(requestTo(HOOK))
                .andRespond(withException(new java.io.IOException("network down")));

        boolean ok = f.client().send(HOOK, DiscordWebhookPayload.of("net-fail"));

        assertThat(ok).isFalse();
    }

    @Test
    void rejectsNullArgs() {
        DiscordWebhookClient client = newFixture().client();
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> client.send(null, DiscordWebhookPayload.of("x")));
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> client.send(HOOK, null));
    }

    @Test
    void payloadOmitsNullUsername() {
        var payload = new DiscordWebhookPayload("hi", null);
        assertThat(payload.username()).isNull();
        assertThat(payload.content()).isEqualTo("hi");
    }
}
