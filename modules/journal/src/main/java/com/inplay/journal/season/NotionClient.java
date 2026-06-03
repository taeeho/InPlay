package com.inplay.journal.season;

import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Notion API {@code POST /v1/pages} 호출로 일지 페이지 생성.
 *
 * <p>brief의 {@code DiscordWebhookClient}와 동일 정책: 4xx/5xx/network 모두 swallow + 로그, boolean 반환.
 * 일지 발송 실패가 스케줄러를 죽이지 않도록.
 */
public class NotionClient {

    static final String PAGES_URL = "https://api.notion.com/v1/pages";

    private static final Logger log = LoggerFactory.getLogger(NotionClient.class);

    private final RestClient http;
    private final String apiKey;
    private final String notionVersion;

    public NotionClient(RestClient http, String apiKey, String notionVersion) {
        this.http = Objects.requireNonNull(http, "http required");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey required");
        this.notionVersion = Objects.requireNonNull(notionVersion, "notionVersion required");
    }

    public boolean createPage(Map<String, Object> requestBody) {
        Objects.requireNonNull(requestBody, "requestBody required");
        try {
            HttpStatusCode status = http.post()
                    .uri(PAGES_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Notion-Version", notionVersion)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            if (status.is2xxSuccessful()) {
                return true;
            }
            log.warn("notion page create returned non-2xx status={}", status);
            return false;
        } catch (RestClientResponseException e) {
            log.warn("notion page create rejected status={} body={}",
                    e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            return false;
        } catch (RuntimeException e) {
            log.warn("notion page create failed: {}", e.toString());
            return false;
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
