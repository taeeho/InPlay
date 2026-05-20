package com.inplay.decision.brief;

import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Pre-game 브리핑 결과. predictor 없거나 history 부족 시 winProbability=null로 "모델 미준비" 상태.
 */
public record WinProbabilityBrief(
        GameId gameId,
        LocalDate date,
        KboTeam homeTeam,
        KboTeam awayTeam,
        Double homeWinProbability,
        KboTeam highlightTeam,
        String note) {

    public WinProbabilityBrief {
        Objects.requireNonNull(gameId, "gameId required");
        Objects.requireNonNull(date, "date required");
        Objects.requireNonNull(homeTeam, "homeTeam required");
        Objects.requireNonNull(awayTeam, "awayTeam required");
        if (homeTeam == awayTeam) {
            throw new IllegalArgumentException("home and away must differ: " + homeTeam);
        }
        if (homeWinProbability != null) {
            double p = homeWinProbability;
            if (Double.isNaN(p) || p < 0.0 || p > 1.0) {
                throw new IllegalArgumentException("homeWinProbability must be in [0,1], got " + p);
            }
        }
    }

    public Optional<Double> probability() {
        return Optional.ofNullable(homeWinProbability);
    }

    public boolean isModelReady() {
        return homeWinProbability != null;
    }
}
