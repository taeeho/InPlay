package com.inplay.decision.clutch;

import com.inplay.core.domain.team.KboTeam;

import java.util.Map;
import java.util.Objects;

/**
 * 사용자별 응원팀 + 라이벌리 가중치 — W4 임시 holder.
 *
 * <p>W6에 User 도메인 + DB 백업이 들어오면 그쪽으로 이동.
 * 현재는 {@code DefaultUserProperties} (api 모듈)에서 application.yml 값으로 주입.
 *
 * @param myTeam          사용자 응원팀. null 가능 (관전자 모드).
 * @param rivalryWeights  팀별 가중치 map. 기본은 1.0, 라이벌 1.2~1.3, 무관 0.7.
 *                        해당 팀이 없는 게임이면 적용 X.
 */
public record RivalrySettings(KboTeam myTeam, Map<KboTeam, Double> rivalryWeights) {
    public RivalrySettings {
        Objects.requireNonNull(rivalryWeights, "rivalryWeights required (use Map.of())");
        rivalryWeights = Map.copyOf(rivalryWeights);
    }

    public static RivalrySettings noPreference() {
        return new RivalrySettings(null, Map.of());
    }

    public double weightFor(KboTeam team) {
        Double w = rivalryWeights.get(team);
        return w == null ? 1.0 : w;
    }
}
