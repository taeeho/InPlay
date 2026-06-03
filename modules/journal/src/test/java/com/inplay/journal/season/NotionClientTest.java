package com.inplay.journal.season;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NotionClientTest {

    private record Fixture(NotionClient client, MockRestServiceServer server) {}

    private Fixture newFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NotionClient client = new NotionClient(builder.build(), "secret-key", "2022-06-28");
        return new Fixture(client, server);
    }

    @Test
    void postsToNotionPagesWithAuthHeaders() {
        Fixture f = newFixture();
        f.server().expect(requestTo(NotionClient.PAGES_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-key"))
                .andExpect(header("Notion-Version", "2022-06-28"))
                .andRespond(withStatus(HttpStatus.OK).body("{\"id\":\"page-1\"}"));

        boolean ok = f.client().createPage(Map.of("parent", Map.of("database_id", "db")));

        assertThat(ok).isTrue();
        f.server().verify();
    }

    @Test
    void returnsFalseOn4xx() {
        Fixture f = newFixture();
        f.server().expect(requestTo(NotionClient.PAGES_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"message\":\"bad schema\"}"));

        assertThat(f.client().createPage(Map.of("x", "y"))).isFalse();
    }

    @Test
    void returnsFalseOn5xx() {
        Fixture f = newFixture();
        f.server().expect(requestTo(NotionClient.PAGES_URL))
                .andRespond(withRawStatus(500).body(""));

        assertThat(f.client().createPage(Map.of("x", "y"))).isFalse();
    }
}
