package com.inplay.notify.discord;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.decision.clutch.ClutchVerdict;
import com.inplay.decision.clutch.ImportanceScore;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class ClutchPushFormatterTest {

    private LiveEvent event(int inning, InningHalf half, int outs, boolean[] runners,
                            int homeRuns, int awayRuns, OptionalDouble wpaAfter) {
        return new LiveEvent(
                Instant.parse("2026-05-12T19:34:21Z"),
                new GameId("g1"),
                inning, half, LiveEventType.PITCH,
                outs, runners, new Score(homeRuns, awayRuns),
                Optional.empty(), Optional.empty(), Optional.empty(),
                wpaAfter, "src", "id");
    }

    private Game game(KboTeam home, KboTeam away, int hr, int ar) {
        return new Game(new GameId("g1"), LocalDate.of(2026, 5, 12),
                home, away, GameStatus.LIVE, new Score(hr, ar));
    }

    @Test
    void formatsLoadedFifthInningHomeLead() {
        var prev = event(5, InningHalf.BOTTOM, 1, new boolean[]{true, true, true}, 3, 3, OptionalDouble.of(0.55));
        var curr = event(5, InningHalf.BOTTOM, 1, new boolean[]{true, true, true}, 4, 3, OptionalDouble.of(0.78));
        var g = game(KboTeam.HH, KboTeam.LG, 4, 3);
        var v = ClutchVerdict.ready(0.85, 0.7);
        var imp = new ImportanceScore(7.5, 0.46, 1.3, 0.6);

        String body = ClutchPushFormatter.buildContent(prev, curr, g, v, imp);
        assertThat(body).contains("5회말");
        assertThat(body).contains("만루");
        assertThat(body).contains("1아웃");
        assertThat(body).contains("0.55");
        assertThat(body).contains("0.78");
        assertThat(body).contains("+0.23");
        assertThat(body).contains("7.5/10");
        assertThat(body).contains("clutch 0.85");
        assertThat(body).contains("**HH** 4-3 LG");
    }

    @Test
    void firstEventOfGameUsesHalfAsBaseline() {
        var curr = event(1, InningHalf.TOP, 0, new boolean[3], 0, 0, OptionalDouble.of(0.60));
        var g = game(KboTeam.HH, KboTeam.LG, 0, 0);
        var v = ClutchVerdict.ready(0.71, 0.7);
        var imp = new ImportanceScore(4.0, 0.2, 1.0, 0.4);

        String body = ClutchPushFormatter.buildContent(null, curr, g, v, imp);
        assertThat(body).contains("승률 0.50 → 0.60");
        assertThat(body).contains("주자없음");
        assertThat(body).contains("0아웃");
    }

    @Test
    void awayLeadingBoldsAwayTeam() {
        var prev = event(7, InningHalf.TOP, 2, new boolean[3], 3, 4, OptionalDouble.of(0.42));
        var curr = event(7, InningHalf.TOP, 2, new boolean[3], 3, 5, OptionalDouble.of(0.28));
        var g = game(KboTeam.HH, KboTeam.LG, 3, 5);
        var v = ClutchVerdict.ready(0.80, 0.7);
        var imp = new ImportanceScore(6.1, 0.28, 1.2, 0.55);

        String body = ClutchPushFormatter.buildContent(prev, curr, g, v, imp);
        assertThat(body).contains("HH 3-5 **LG**");
        assertThat(body).contains("7회초");
        assertThat(body).contains("-0.14");
    }

    @Test
    void modelNotReadyDropsClutchClause() {
        var prev = event(5, InningHalf.BOTTOM, 1, new boolean[3], 3, 3, OptionalDouble.of(0.50));
        var curr = event(5, InningHalf.BOTTOM, 1, new boolean[]{true, false, false}, 3, 3, OptionalDouble.of(0.55));
        var g = game(KboTeam.HH, KboTeam.LG, 3, 3);
        var v = ClutchVerdict.modelNotReady(0.7);
        var imp = new ImportanceScore(3.2, 0.1, 1.0, 0.32);

        String body = ClutchPushFormatter.buildContent(prev, curr, g, v, imp);
        assertThat(body).contains("3.2/10");
        assertThat(body).doesNotContain("clutch");
    }

    @Test
    void payloadCarriesContent() {
        var curr = event(5, InningHalf.BOTTOM, 1, new boolean[3], 3, 3, OptionalDouble.of(0.6));
        var p = ClutchPushFormatter.format(null, curr,
                game(KboTeam.HH, KboTeam.LG, 3, 3),
                ClutchVerdict.ready(0.8, 0.7),
                new ImportanceScore(5.0, 0.2, 1.0, 0.4));
        assertThat(p.content()).contains("중요도 5.0/10");
    }
}
