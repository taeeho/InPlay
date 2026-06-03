package com.inplay.journal.season;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.team.KboTeam;
import java.util.Objects;
import java.util.Optional;

/**
 * 종료된 경기 + 사용자 응원팀 → {@link SeasonJournalEntry}.
 *
 * <p>경기가 {@link GameStatus#FINAL}이 아니거나 myTeam이 그 경기에 출전하지 않으면 {@link Optional#empty()}.
 */
public final class JournalEntryGenerator {

    public Optional<SeasonJournalEntry> generate(Game game, KboTeam myTeam, String userId) {
        Objects.requireNonNull(game, "game required");
        Objects.requireNonNull(myTeam, "myTeam required");
        Objects.requireNonNull(userId, "userId required");

        if (game.status() != GameStatus.FINAL) {
            return Optional.empty();
        }
        if (game.homeTeam() != myTeam && game.awayTeam() != myTeam) {
            return Optional.empty();
        }

        int home = game.score().home();
        int away = game.score().away();
        String summary = "%s %d:%d %s".formatted(game.homeTeam().code(), home, away, game.awayTeam().code());

        return Optional.of(new SeasonJournalEntry(
                userId,
                game.date().getYear(),
                game.id().value(),
                game.date(),
                game.homeTeam(),
                game.awayTeam(),
                home,
                away,
                myTeam,
                outcomeFor(myTeam, game.homeTeam(), home, away),
                summary));
    }

    private static JournalOutcome outcomeFor(KboTeam myTeam, KboTeam homeTeam, int home, int away) {
        if (home == away) {
            return JournalOutcome.DRAW;
        }
        boolean homeWon = home > away;
        boolean myTeamIsHome = myTeam == homeTeam;
        return homeWon == myTeamIsHome ? JournalOutcome.WIN : JournalOutcome.LOSS;
    }
}
