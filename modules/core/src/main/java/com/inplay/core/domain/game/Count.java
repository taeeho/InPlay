package com.inplay.core.domain.game;

public record Count(int balls, int strikes) {
    public Count {
        if (balls < 0 || balls > 3) {
            throw new IllegalArgumentException("balls must be in 0..3, got " + balls);
        }
        if (strikes < 0 || strikes > 2) {
            throw new IllegalArgumentException("strikes must be in 0..2, got " + strikes);
        }
    }

    public static Count fresh() {
        return new Count(0, 0);
    }
}
