package com.inplay.core.domain.team;

import java.util.Map;
import java.util.Objects;

public record MyTeam(KboTeam myTeam, Map<KboTeam, Double> rivalryWeights) {
    public MyTeam {
        Objects.requireNonNull(myTeam, "myTeam required");
        Objects.requireNonNull(rivalryWeights, "rivalryWeights required");
        if (rivalryWeights.containsKey(myTeam)) {
            throw new IllegalArgumentException("rivalry_weights must not contain my_team itself: " + myTeam);
        }
        for (Map.Entry<KboTeam, Double> entry : rivalryWeights.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "rivalry team key must not be null");
            Double weight = entry.getValue();
            if (weight == null || weight <= 0.0) {
                throw new IllegalArgumentException(
                        "rivalry weight must be > 0 (team=" + entry.getKey() + ", weight=" + weight + ")");
            }
        }
        rivalryWeights = Map.copyOf(rivalryWeights);
    }
}
