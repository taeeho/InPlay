package com.inplay.decision.clutch;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.team.KboTeam;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Importance score = wpaChange × rivalryWeight × leverage (PLAN.md §6).
 *
 * <p>Output is normalized to [0, 10] so {@link ClutchNotifyPolicy} can apply a single threshold.
 *
 * <p>Rivalry weight resolution (둘 다 my_team이 아닌 게임은 관심도 ↓):
 *   - my_team이 게임에 포함되지 않으면: 양 팀의 weightFor의 max (라이벌 매치업도 가중)
 *     없으면 0.7 (관심도 ↓ 신호)
 *   - my_team이 한쪽이면: 상대 팀의 weightFor(라이벌만 ↑)
 *   - my_team이 없으면: 1.0
 */
public final class ImportanceScorer {

    private static final double WPA_CHANGE_REFERENCE = 0.5;     // 50%p swing은 max
    private static final double LEVERAGE_DIVISOR = 1.5;          // ClutchFeatures.leverageProxy max
    private static final double NEUTRAL_WEIGHT = 1.0;
    // 비관심 경기(my_team이 없고 라이벌리 보너스도 없는)는 0.5 — leverage+wpa 1.0이어도
    // raw = 1.0 × 0.5 × 1.0 = 0.5 → 5점이라 threshold 3.0보다 위. 의도적: 본인 사용자가
    // "한화팬이지만 LG-KIA도 본다"를 위한 floor. 완전히 끄려면 user 도메인에서 mute 필요.
    private static final double NO_MY_TEAM_FLOOR = 0.5;

    public ImportanceScore score(ClutchVerdict verdict, Game game, LiveEvent previous,
                                 LiveEvent current, RivalrySettings settings) {
        Objects.requireNonNull(verdict, "verdict required");
        Objects.requireNonNull(game, "game required");
        Objects.requireNonNull(current, "current required");
        Objects.requireNonNull(settings, "settings required");

        if (current.wpaAfter().isEmpty() || verdict.probability() == null) {
            return ImportanceScore.zero();
        }

        double weAfter = current.wpaAfter().getAsDouble();
        double weBefore = previousWe(previous);
        double wpaChange = Math.min(Math.abs(weAfter - weBefore) / WPA_CHANGE_REFERENCE, 1.0);

        double leverage = leverageFor(verdict, previous, current);
        double rivalry = rivalryWeight(game, settings);

        double raw = wpaChange * rivalry * leverage;
        double scaled = Math.min(raw * 10.0, 10.0);

        return new ImportanceScore(scaled, wpaChange, rivalry, leverage);
    }

    private static double previousWe(LiveEvent prev) {
        if (prev == null) return 0.5;
        OptionalDouble v = prev.wpaAfter();
        return v.isPresent() ? v.getAsDouble() : 0.5;
    }

    private static double leverageFor(ClutchVerdict verdict, LiveEvent prev, LiveEvent curr) {
        // verdict.probability already encodes leverage indirectly via the LightGBM model,
        // but we keep an explicit leverage proxy to make threshold tuning interpretable.
        double weAfter = curr.wpaAfter().getAsDouble();
        double weBefore = previousWe(prev);
        double wpaChangeAbs = Math.abs(weAfter - weBefore);
        double inningProgress = Math.min(curr.inning(), 9) / 9.0;
        int runnersOn = (curr.runnerOn(1) ? 1 : 0)
                + (curr.runnerOn(2) ? 1 : 0)
                + (curr.runnerOn(3) ? 1 : 0);
        double runnersNorm = runnersOn / 3.0;
        double leverageProxy = wpaChangeAbs * inningProgress * (1.0 + 0.5 * runnersNorm);
        return Math.min(leverageProxy / LEVERAGE_DIVISOR, 1.0);
    }

    private static double rivalryWeight(Game game, RivalrySettings settings) {
        KboTeam my = settings.myTeam();
        KboTeam home = game.homeTeam();
        KboTeam away = game.awayTeam();
        if (my == null) {
            return NEUTRAL_WEIGHT;
        }
        if (home == my) {
            return Math.max(settings.weightFor(away), NEUTRAL_WEIGHT);
        }
        if (away == my) {
            return Math.max(settings.weightFor(home), NEUTRAL_WEIGHT);
        }
        // my_team이 게임에 없음 — 라이벌끼리 붙으면 보너스, 평범한 매치면 floor
        double rival = Math.max(settings.weightFor(home), settings.weightFor(away));
        if (rival > NEUTRAL_WEIGHT) {
            return rival * 0.85; // 라이벌 매치업이지만 내 팀 X → 살짝 감산
        }
        return NO_MY_TEAM_FLOOR;
    }
}
