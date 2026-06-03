package com.inplay.journal.season;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotionJournalPageTest {

    private SeasonJournalEntry entry(JournalOutcome outcome) {
        return new SeasonJournalEntry(
                "taeeho", 2026, "g1", LocalDate.of(2026, 6, 3),
                KboTeam.HH, KboTeam.LG, 5, 3, KboTeam.HH, outcome, "HH 5:3 LG");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsParentAndProperties() {
        Map<String, Object> body = NotionJournalPage.requestBody(entry(JournalOutcome.WIN), "db123");

        assertThat(((Map<String, Object>) body.get("parent")).get("database_id")).isEqualTo("db123");

        Map<String, Object> props = (Map<String, Object>) body.get("properties");
        assertThat(props).containsKeys(
                NotionJournalPage.COL_TITLE, NotionJournalPage.COL_DATE,
                NotionJournalPage.COL_SEASON, NotionJournalPage.COL_RESULT, NotionJournalPage.COL_SCORE);

        Map<String, Object> result = (Map<String, Object>) props.get(NotionJournalPage.COL_RESULT);
        assertThat(((Map<String, Object>) result.get("select")).get("name")).isEqualTo("승");

        Map<String, Object> season = (Map<String, Object>) props.get(NotionJournalPage.COL_SEASON);
        assertThat(season.get("number")).isEqualTo(2026);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapsLossAndDrawLabels() {
        Map<String, Object> loss = (Map<String, Object>)
                ((Map<String, Object>) NotionJournalPage.requestBody(entry(JournalOutcome.LOSS), "db")
                        .get("properties")).get(NotionJournalPage.COL_RESULT);
        assertThat(((Map<String, Object>) loss.get("select")).get("name")).isEqualTo("패");

        Map<String, Object> draw = (Map<String, Object>)
                ((Map<String, Object>) NotionJournalPage.requestBody(entry(JournalOutcome.DRAW), "db")
                        .get("properties")).get(NotionJournalPage.COL_RESULT);
        assertThat(((Map<String, Object>) draw.get("select")).get("name")).isEqualTo("무");
    }
}
