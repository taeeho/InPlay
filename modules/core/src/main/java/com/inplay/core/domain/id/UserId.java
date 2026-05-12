package com.inplay.core.domain.id;

import java.util.Objects;

public record UserId(String value) {
    public UserId {
        Objects.requireNonNull(value, "user_id value required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("user_id must not be blank");
        }
    }
}
