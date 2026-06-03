package com.inplay.journal.season;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link SeasonJournalEntry} → Notion {@code POST /v1/pages} request body.
 *
 * <p>아래 property 칼럼명은 사용자가 Notion {@code season_journal} DB를 만들 때 맞춰야 한다(HANDOFF 참조).
 * 스키마 불일치 시 Notion이 4xx 반환 → {@link NotionClient}가 swallow + 로그.
 */
public final class NotionJournalPage {

    // season_journal DB 칼럼명 (Notion UI에서 동일하게 생성).
    static final String COL_TITLE = "경기";
    static final String COL_DATE = "날짜";
    static final String COL_SEASON = "시즌";
    static final String COL_RESULT = "결과";
    static final String COL_SCORE = "스코어";

    private NotionJournalPage() {}

    public static Map<String, Object> requestBody(SeasonJournalEntry entry, String databaseId) {
        Objects.requireNonNull(entry, "entry required");
        Objects.requireNonNull(databaseId, "databaseId required");

        Map<String, Object> properties = Map.of(
                COL_TITLE, title(entry.summary()),
                COL_DATE, date(entry.date().toString()),
                COL_SEASON, number(entry.season()),
                COL_RESULT, select(resultLabel(entry.outcome())),
                COL_SCORE, richText(entry.homeScore() + ":" + entry.awayScore()));

        return Map.of(
                "parent", Map.of("database_id", databaseId),
                "properties", properties);
    }

    private static Map<String, Object> title(String content) {
        return Map.of("title", List.of(Map.of("text", Map.of("content", content))));
    }

    private static Map<String, Object> richText(String content) {
        return Map.of("rich_text", List.of(Map.of("text", Map.of("content", content))));
    }

    private static Map<String, Object> date(String isoDate) {
        return Map.of("date", Map.of("start", isoDate));
    }

    private static Map<String, Object> number(int value) {
        return Map.of("number", value);
    }

    private static Map<String, Object> select(String name) {
        return Map.of("select", Map.of("name", name));
    }

    private static String resultLabel(JournalOutcome outcome) {
        return switch (outcome) {
            case WIN -> "승";
            case LOSS -> "패";
            case DRAW -> "무";
        };
    }
}
