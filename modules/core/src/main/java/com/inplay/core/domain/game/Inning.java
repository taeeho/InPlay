package com.inplay.core.domain.game;

import java.util.Objects;

public record Inning(int number, InningHalf half) {
    public Inning {
        if (number < 1) {
            throw new IllegalArgumentException("inning number must be >= 1, got " + number);
        }
        Objects.requireNonNull(half, "half required");
    }
}
