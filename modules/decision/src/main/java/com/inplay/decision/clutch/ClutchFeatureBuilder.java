package com.inplay.decision.clutch;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.inference.clutch.ClutchFeatures;

import java.util.Objects;
import java.util.Optional;

/**
 * LiveEvent (prev, curr) → ClutchFeatures.
 *
 * <p>Python {@code clutch/features.py} 의 7 feature 수식과 1:1 일치 — parity 유지.
 *
 * <p>prev null (게임 첫 이벤트): we_home_before=0.5 가정 → wpa_change_abs = |we_home_after - 0.5|.
 * curr.wpaAfter empty (WpaAnnotator 미실행) → {@link Optional#empty()}.
 */
public final class ClutchFeatureBuilder {

    public Optional<ClutchFeatures> build(LiveEvent previous, LiveEvent current) {
        Objects.requireNonNull(current, "current required");
        if (current.wpaAfter().isEmpty()) {
            return Optional.empty();
        }
        double weAfter = current.wpaAfter().getAsDouble();
        double weBefore = (previous == null || previous.wpaAfter().isEmpty())
                ? 0.5
                : previous.wpaAfter().getAsDouble();

        double wpaChangeAbs = Math.abs(weAfter - weBefore);
        double weBalance = 1.0 - 2.0 * Math.abs(weAfter - 0.5);
        double inningProgress = Math.min(current.inning(), 9) / 9.0;
        int scoreMargin = Math.abs(current.score().home() - current.score().away());
        double scoreMarginNorm = Math.min(scoreMargin, 5) / 5.0;
        int runnersOn = (current.runnerOn(1) ? 1 : 0)
                + (current.runnerOn(2) ? 1 : 0)
                + (current.runnerOn(3) ? 1 : 0);
        double runnersOnNorm = runnersOn / 3.0;
        double outsNorm = Math.min(current.outs(), 3) / 3.0;
        double leverageProxy = wpaChangeAbs * inningProgress * (1.0 + 0.5 * runnersOnNorm);

        return Optional.of(new ClutchFeatures(
                clamp01(wpaChangeAbs),
                clamp01(weBalance),
                inningProgress,
                scoreMarginNorm,
                runnersOnNorm,
                outsNorm,
                Math.min(leverageProxy, 1.5)));
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
