package com.inplay.collector.http;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CollectorHttpConfig {

    @Bean
    public RestClient.Builder collectorRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, CollectorUserAgent.VALUE);
    }

    @Bean
    public RestClient collectorRestClient(RestClient.Builder collectorRestClientBuilder) {
        return collectorRestClientBuilder.build();
    }
}
