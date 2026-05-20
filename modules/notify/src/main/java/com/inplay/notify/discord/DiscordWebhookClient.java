package com.inplay.notify.discord;

import java.net.URI;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Discord webhook 발송 클라이언트.
 *
 * <p>webhook URL은 user별 분리 — 본인 default는 {@code .env}의 {@code INPLAY_DEFAULT_DISCORD_WEBHOOK},
 * 친구는 user 컬렉션에 저장 (W7).
 *
 * <p>네트워크 오류·4xx·5xx 는 swallow + 로그. brief 발송 실패가 스케줄러를 죽이지 않도록.
 * 호출자가 결과를 확인하려면 {@link #send} return 값을 사용.
 */
public class DiscordWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(DiscordWebhookClient.class);

    private final RestClient http;

    public DiscordWebhookClient(RestClient http) {
        this.http = Objects.requireNonNull(http, "http required");
    }

    public boolean send(URI webhookUrl, DiscordWebhookPayload payload) {
        Objects.requireNonNull(webhookUrl, "webhookUrl required");
        Objects.requireNonNull(payload, "payload required");
        try {
            HttpStatusCode status = http.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            if (status.is2xxSuccessful()) {
                return true;
            }
            log.warn("discord webhook returned non-2xx status={}", status);
            return false;
        } catch (RestClientResponseException e) {
            log.warn("discord webhook rejected status={} body={}", e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            return false;
        } catch (RuntimeException e) {
            log.warn("discord webhook send failed: {}", e.toString());
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
