package com.inplay.api.journal;

import com.inplay.journal.season.JournalEntryGenerator;
import com.inplay.journal.season.NotionClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NotionProperties.class)
public class JournalConfig {

    @Bean
    public RestClient notionRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        return RestClient.builder().requestFactory(factory).build();
    }

    @Bean
    public NotionClient notionClient(RestClient notionRestClient, NotionProperties props) {
        String apiKey = props.apiKey() == null ? "" : props.apiKey();
        return new NotionClient(notionRestClient, apiKey, props.version());
    }

    @Bean
    public JournalEntryGenerator journalEntryGenerator() {
        return new JournalEntryGenerator();
    }
}
