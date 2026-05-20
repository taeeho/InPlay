package com.inplay.decision.brief;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.inference.winprob.WinProbabilityFeatures;
import com.inplay.inference.winprob.WinProbabilityPredictor;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Game + history + (선택) predictor → WinProbabilityBrief 생성.
 * predictor가 null이거나 feature 부족 시 prob=null brief 반환 — "모델 미준비" 모드.
 */
public final class BriefGenerator {

    private final WinProbabilityFeatureBuilder featureBuilder;
    private final WinProbabilityPredictor predictor;

    public BriefGenerator(WinProbabilityFeatureBuilder featureBuilder, WinProbabilityPredictor predictor) {
        this.featureBuilder = Objects.requireNonNull(featureBuilder, "featureBuilder required");
        this.predictor = predictor;
    }

    public WinProbabilityBrief generate(Game game, List<Game> seasonHistory, KboTeam myTeam) {
        Objects.requireNonNull(game, "game required");
        Objects.requireNonNull(seasonHistory, "seasonHistory required");

        KboTeam highlight = pickHighlight(game, myTeam);

        if (predictor == null) {
            return new WinProbabilityBrief(
                    game.id(), game.date(), game.homeTeam(), game.awayTeam(),
                    null, highlight, "모델 미준비 (ONNX 산출물 없음)");
        }

        Optional<WinProbabilityFeatures> features = featureBuilder.build(game, seasonHistory);
        if (features.isEmpty()) {
            return new WinProbabilityBrief(
                    game.id(), game.date(), game.homeTeam(), game.awayTeam(),
                    null, highlight, "데이터 부족 (시즌 초반)");
        }

        double prob = predictor.predict(features.get());
        return new WinProbabilityBrief(
                game.id(), game.date(), game.homeTeam(), game.awayTeam(),
                prob, highlight, null);
    }

    private static KboTeam pickHighlight(Game game, KboTeam myTeam) {
        if (myTeam == null) {
            return null;
        }
        if (game.homeTeam() == myTeam || game.awayTeam() == myTeam) {
            return myTeam;
        }
        return null;
    }
}
