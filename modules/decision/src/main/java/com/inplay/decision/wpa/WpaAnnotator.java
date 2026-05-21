package com.inplay.decision.wpa;

import com.inplay.core.domain.event.LiveEvent;

import java.util.Objects;

// Stamps `wpa_after` on a LiveEvent.
//
// Convention (PLAN.md §3 schema): wpa_after = WE_home AFTER the play.
//   - Absolute home-team win probability, in [0.0, 1.0]
//   - NOT a delta; downstream consumers compute ΔWE by subtracting from previous event's wpa_after.
//
// Idempotent: re-annotating an already-stamped event overwrites with a fresh WE
//   (so polling re-emits with new score don't drift).
//
// Known limits (W3 MVP, calibration TODO):
//   - LiveEventStateMapper hardcodes gameOver=false, so the final event of a game does NOT
//     converge to {0,1} WE. ΔWE summed across all events of a finished game will fall short of
//     the true outcome by the residual WE. Add a `finalize(LiveEvent endEvent, GameOutcome)`
//     pass in W4 to set gameOver=true on the terminal event.
//   - Model version is not stamped. Once we hot-swap WE/RE24 to KBO-calibrated values,
//     historical wpa_after values will not be self-describing — add a `model_version` field
//     on `live_event` before the first KBO calibration cutover.
public final class WpaAnnotator {

    public LiveEvent annotate(LiveEvent event) {
        Objects.requireNonNull(event, "event required");
        GameState state = LiveEventStateMapper.toGameState(event);
        double weHome = WinExpectancy.homeWinProb(state);
        return event.withWpaAfter(weHome);
    }
}
