package com.inplay.api.dashboard;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;

public record DashboardRow(
        String gameId,
        LocalDate date,
        KboTeam homeTeam,
        KboTeam awayTeam,
        String status,
        int homeScore,
        int awayScore,
        String briefMarkdown,
        boolean modelReady) {

    public static DashboardRow of(Game g, String brief, boolean modelReady) {
        return new DashboardRow(
                g.id().value(),
                g.date(),
                g.homeTeam(),
                g.awayTeam(),
                g.status().name(),
                g.score().home(),
                g.score().away(),
                brief,
                modelReady);
    }

    public boolean isFinal() {
        return "FINAL".equals(status);
    }
}
