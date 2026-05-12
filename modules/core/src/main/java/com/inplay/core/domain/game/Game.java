package com.inplay.core.domain.game;

import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.util.Objects;

public record Game(
        GameId id,
        LocalDate date,
        KboTeam homeTeam,
        KboTeam awayTeam,
        GameStatus status,
        Score score) {
    public Game {
        Objects.requireNonNull(id, "id required");
        Objects.requireNonNull(date, "date required");
        Objects.requireNonNull(homeTeam, "homeTeam required");
        Objects.requireNonNull(awayTeam, "awayTeam required");
        Objects.requireNonNull(status, "status required");
        Objects.requireNonNull(score, "score required");
        if (homeTeam == awayTeam) {
            throw new IllegalArgumentException("home and away teams must differ: " + homeTeam);
        }
    }
}
