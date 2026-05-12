package com.inplay.core.domain.team;

import java.util.Objects;

public record Team(KboTeam code, String fullName) {
    public Team {
        Objects.requireNonNull(code, "code required");
        Objects.requireNonNull(fullName, "fullName required");
        if (fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
    }
}
