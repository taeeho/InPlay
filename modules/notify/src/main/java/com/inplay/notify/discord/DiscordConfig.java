package com.inplay.notify.discord;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DiscordConfig {

    @Bean
    public RestClient discordRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        return RestClient.builder().requestFactory(factory).build();
    }

    @Bean
    public DiscordWebhookClient discordWebhookClient(RestClient discordRestClient) {
        return new DiscordWebhookClient(discordRestClient);
    }
}
