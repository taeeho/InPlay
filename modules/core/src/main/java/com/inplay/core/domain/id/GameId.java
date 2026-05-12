package com.inplay.core.domain.id;

import java.util.Objects;

public record GameId(String value) {
    public GameId {
        Objects.requireNonNull(value, "game_id value required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("game_id must not be blank");
        }
    }
}
