package com.inplay.core.domain.id;

import java.util.Objects;

public record PlayerId(String value) {
    public PlayerId {
        Objects.requireNonNull(value, "player_id value required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("player_id must not be blank");
        }
    }
}
