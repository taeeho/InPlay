package com.inplay.api.brief;

import com.inplay.core.domain.team.KboTeam;
import java.net.URI;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * .env / application.yml의 단일 default user (본인). W7에서 user 컬렉션 기반 multi-tenant로 확장.
 *
 * <p>{@code rivalryWeights}: e.g. {LG: 1.3, KIA: 1.25}. 기본값 (없는 팀) 1.0.
 * {@code muteWindow.start/end}: KST 기준 통근 시간대 (e.g. 08:00/10:00).
 */
@ConfigurationProperties(prefix = "inplay.default-user")
public record DefaultUserProperties(
        String name,
        KboTeam team,
        String timezone,
        URI discordWebhook,
        MuteWindowProperties muteWindow,
        Map<KboTeam, Double> rivalryWeights) {

    public record MuteWindowProperties(String start, String end) {}
}
