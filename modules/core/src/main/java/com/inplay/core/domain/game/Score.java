package com.inplay.core.domain.game;

public record Score(int home, int away) {
    public Score {
        if (home < 0) {
            throw new IllegalArgumentException("home runs must be >= 0, got " + home);
        }
        if (away < 0) {
            throw new IllegalArgumentException("away runs must be >= 0, got " + away);
        }
    }

    public static Score zero() {
        return new Score(0, 0);
    }

    public int margin() {
        return home - away;
    }
}
