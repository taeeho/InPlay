package com.inplay.decision.brief;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.inference.winprob.WinProbabilityFeatures;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Game + 과거 시즌 경기 리스트에서 WinProbabilityFeatures 7개 계산.
 * Python trainer features.py 와 같은 정의·시간 누설 방지 (target.date 이전만 사용).
 *
 * <p>cold-start(history 부족)인 경우 {@link Optional#empty()} — predictor 호출하지 말 것.
 */
public final class WinProbabilityFeatureBuilder {

    private static final int RECENT_WINDOW = 10;
    private static final int MIN_HISTORY_PER_TEAM = 3;

    public Optional<WinProbabilityFeatures> build(Game target, List<Game> seasonHistory) {
        Objects.requireNonNull(target, "target required");
        Objects.requireNonNull(seasonHistory, "seasonHistory required");

        List<Game> past = seasonHistory.stream()
                .filter(g -> g.status() == GameStatus.FINAL)
                .filter(g -> g.date().isBefore(target.date()))
                .sorted(Comparator.comparing(Game::date))
                .toList();

        KboTeam home = target.homeTeam();
        KboTeam away = target.awayTeam();

        List<Game> homeAll = past.stream().filter(g -> involves(g, home)).toList();
        List<Game> awayAll = past.stream().filter(g -> involves(g, away)).toList();

        if (homeAll.size() < MIN_HISTORY_PER_TEAM || awayAll.size() < MIN_HISTORY_PER_TEAM) {
            return Optional.empty();
        }

        double homeSeason = winRate(homeAll, home);
        double awaySeason = winRate(awayAll, away);
        double homeAtHome = winRate(homeAll.stream().filter(g -> g.homeTeam() == home).toList(), home);
        double awayAtAway = winRate(awayAll.stream().filter(g -> g.awayTeam() == away).toList(), away);
        double homeRecent = winRate(takeLast(homeAll, RECENT_WINDOW), home);
        double awayRecent = winRate(takeLast(awayAll, RECENT_WINDOW), away);

        List<Game> h2h = past.stream()
                .filter(g -> involves(g, home) && involves(g, away))
                .toList();
        double h2hHome = h2h.isEmpty() ? 0.5 : winRate(h2h, home);

        return Optional.of(new WinProbabilityFeatures(
                homeSeason, awaySeason, homeAtHome, awayAtAway,
                homeRecent, awayRecent, h2hHome));
    }

    private static boolean involves(Game g, KboTeam team) {
        return g.homeTeam() == team || g.awayTeam() == team;
    }

    private static double winRate(List<Game> games, KboTeam team) {
        if (games.isEmpty()) {
            return 0.5;
        }
        long wins = games.stream().filter(g -> isWinner(g, team)).count();
        return (double) wins / games.size();
    }

    private static boolean isWinner(Game g, KboTeam team) {
        int margin = g.score().margin();
        if (margin == 0) {
            return false;
        }
        boolean homeWon = margin > 0;
        return (g.homeTeam() == team) == homeWon;
    }

    private static <T> List<T> takeLast(List<T> list, int n) {
        if (list.size() <= n) {
            return list;
        }
        return list.subList(list.size() - n, list.size());
    }
}
