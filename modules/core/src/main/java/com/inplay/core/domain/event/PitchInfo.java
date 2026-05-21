package com.inplay.core.domain.event;

import java.util.Objects;

public record PitchInfo(String type, int speedKmh, String result) {
    public PitchInfo {
        Objects.requireNonNull(type, "pitch type required");
        Objects.requireNonNull(result, "pitch result required");
        if (type.isBlank()) {
            throw new IllegalArgumentException("pitch type must not be blank");
        }
        if (result.isBlank()) {
            throw new IllegalArgumentException("pitch result must not be blank");
        }
        if (speedKmh < 0 || speedKmh > 200) {
            throw new IllegalArgumentException("speedKmh must be in 0..200, got " + speedKmh);
        }
    }
}
