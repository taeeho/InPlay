package com.inplay.api.brief;

import com.inplay.core.domain.team.KboTeam;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * .env / application.yml의 단일 default user (본인). W7에서 user 컬렉션 기반 multi-tenant로 확장.
 */
@ConfigurationProperties(prefix = "inplay.default-user")
public record DefaultUserProperties(
        String name,
        KboTeam team,
        String timezone,
        URI discordWebhook) {
}
