package com.inplay.journal.season;

import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 한 경기에 대한 사용자 시점 시즌 일지 한 건. Notion {@code season_journal} DB의 한 row가 된다.
 *
 * <p>중복 키는 {@code (userId, season, gameId)} (mongo unique index {@code uniq_user_season_game}).
 * {@code summary}는 팀 코드 기반 중립 표현(예: "HH 5:3 LG") — i18n 위해 팀명 한글 하드코딩 X.
 */
public record SeasonJournalEntry(
        String userId,
        int season,
        String gameId,
        LocalDate date,
        KboTeam homeTeam,
        KboTeam awayTeam,
        int homeScore,
        int awayScore,
        KboTeam myTeam,
        JournalOutcome outcome,
        String summary) {

    public SeasonJournalEntry {
        Objects.requireNonNull(userId, "userId required");
        Objects.requireNonNull(gameId, "gameId required");
        Objects.requireNonNull(date, "date required");
        Objects.requireNonNull(homeTeam, "homeTeam required");
        Objects.requireNonNull(awayTeam, "awayTeam required");
        Objects.requireNonNull(myTeam, "myTeam required");
        Objects.requireNonNull(outcome, "outcome required");
        Objects.requireNonNull(summary, "summary required");
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("scores must be >= 0");
        }
        if (myTeam != homeTeam && myTeam != awayTeam) {
            throw new IllegalArgumentException("myTeam must be one of the two teams: " + myTeam);
        }
    }
}
