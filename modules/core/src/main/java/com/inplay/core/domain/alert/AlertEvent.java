package com.inplay.core.domain.alert;

import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.id.UserId;
import java.time.Instant;
import java.util.Objects;

public record AlertEvent(
        UserId userId,
        GameId gameId,
        Instant ts,
        AlertCategory category,
        double importance,
        String message) {
    public AlertEvent {
        Objects.requireNonNull(userId, "userId required");
        Objects.requireNonNull(gameId, "gameId required");
        Objects.requireNonNull(ts, "ts required");
        Objects.requireNonNull(category, "category required");
        Objects.requireNonNull(message, "message required");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        if (Double.isNaN(importance) || importance < 0.0 || importance > 10.0) {
            throw new IllegalArgumentException("importance must be in 0..10, got " + importance);
        }
    }
}
