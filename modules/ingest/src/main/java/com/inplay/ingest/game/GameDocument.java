package com.inplay.ingest.game;

import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "game")
public record GameDocument(
        @Id String id,
        @Field("game_id") String gameId,
        @Field("date") LocalDate date,
        @Field("home_team") String homeTeam,
        @Field("away_team") String awayTeam,
        @Field("status") String status,
        @Field("score") ScoreDocument score) {

    public static GameDocument forNew(
            String gameId,
            LocalDate date,
            String homeTeam,
            String awayTeam,
            String status,
            ScoreDocument score) {
        return new GameDocument(null, gameId, date, homeTeam, awayTeam, status, score);
    }

    public GameDocument withId(String id) {
        return new GameDocument(id, gameId, date, homeTeam, awayTeam, status, score);
    }

    public record ScoreDocument(int home, int away) {}
}
