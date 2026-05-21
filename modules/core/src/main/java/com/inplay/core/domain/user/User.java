package com.inplay.core.domain.user;

import com.inplay.core.domain.id.UserId;
import com.inplay.core.domain.team.KboTeam;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * inplay 사용자 (PLAN.md §3 user schema).
 *
 * <p>apiKeyHash 는 sha256(raw_api_key)의 64자 hex. 원본 키는 발급 직후에만 외부로 노출, 저장 X.
 * rivalryWeights 는 immutable copy — 한 팀 default 1.0, 라이벌 1.2~1.5, 비호감 < 1.0.
 */
public record User(
        UserId userId,
        String name,
        String apiKeyHash,
        KboTeam myTeam,
        Map<KboTeam, Double> rivalryWeights,
        URI discordWebhookUrl,
        UserMuteWindow muteWindow,
        Instant createdAt
) {
    public User {
        Objects.requireNonNull(userId, "userId required");
        Objects.requireNonNull(apiKeyHash, "apiKeyHash required");
        Objects.requireNonNull(myTeam, "myTeam required");
        Objects.requireNonNull(rivalryWeights, "rivalryWeights required (use Map.of())");
        Objects.requireNonNull(muteWindow, "muteWindow required");
        Objects.requireNonNull(createdAt, "createdAt required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (apiKeyHash.length() != 64) {
            throw new IllegalArgumentException("apiKeyHash must be 64-char sha256 hex, got len=" + apiKeyHash.length());
        }
        if (!apiKeyHash.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("apiKeyHash must be hex");
        }
        // rivalry weight 범위 검증 — importance scoring 이상값 방지 (Codex 검토).
        for (var e : rivalryWeights.entrySet()) {
            double w = e.getValue();
            if (Double.isNaN(w) || w < 0.0 || w > 5.0) {
                throw new IllegalArgumentException(
                        "rivalry weight for " + e.getKey() + " must be in [0,5], got " + w);
            }
        }
        rivalryWeights = Map.copyOf(rivalryWeights);
    }

    public double weightFor(KboTeam team) {
        Double w = rivalryWeights.get(team);
        return w == null ? 1.0 : w;
    }
}
