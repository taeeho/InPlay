package com.inplay.decision.wpa;

import com.inplay.core.domain.event.LiveEvent;

import java.util.Objects;

// Maps a LiveEvent snapshot to a GameState consumable by WinExpectancy / WpaCalculator.
//
// gameOver heuristic (MVP, W3): always false.
//   Walk-off / regulation-end detection requires inning context the LiveEvent alone does not carry.
//   Caller (game-finalization service in W4+) is expected to set gameOver explicitly when needed.
public final class LiveEventStateMapper {

    private LiveEventStateMapper() {}

    public static GameState toGameState(LiveEvent e) {
        Objects.requireNonNull(e, "event required");
        boolean[] r = e.runners();
        Bases bases = new Bases(r[0], r[1], r[2]);
        return new GameState(
                e.inning(),
                e.half(),
                e.outs(),
                bases,
                e.score().home(),
                e.score().away(),
                false);
    }
}
