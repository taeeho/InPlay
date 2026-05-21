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
public final class WpaAnnotator {

    public LiveEvent annotate(LiveEvent event) {
        Objects.requireNonNull(event, "event required");
        GameState state = LiveEventStateMapper.toGameState(event);
        double weHome = WinExpectancy.homeWinProb(state);
        return event.withWpaAfter(weHome);
    }
}
