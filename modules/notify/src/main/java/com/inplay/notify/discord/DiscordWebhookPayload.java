package com.inplay.notify.discord;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscordWebhookPayload(String content, String username) {
    public static DiscordWebhookPayload of(String content) {
        return new DiscordWebhookPayload(content, "inplay");
    }
}
