package com.inplay.core.domain.player;

import com.inplay.core.domain.id.PlayerId;
import com.inplay.core.domain.team.KboTeam;
import java.util.Objects;

public record Player(PlayerId id, KboTeam team, String name) {
    public Player {
        Objects.requireNonNull(id, "id required");
        Objects.requireNonNull(team, "team required");
        Objects.requireNonNull(name, "name required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
