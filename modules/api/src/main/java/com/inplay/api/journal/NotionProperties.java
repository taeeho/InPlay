package com.inplay.api.journal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Notion 연동 설정. {@code apiKey}/{@code journalDatabaseId} 미설정이면 일지 생성은 skip.
 */
@ConfigurationProperties(prefix = "inplay.notion")
public record NotionProperties(
        String apiKey,
        String journalDatabaseId,
        String version) {

    public NotionProperties {
        if (version == null || version.isBlank()) {
            version = "2022-06-28";
        }
    }
}
