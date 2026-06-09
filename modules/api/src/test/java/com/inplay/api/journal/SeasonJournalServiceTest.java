package com.inplay.api.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inplay.api.brief.DefaultUserProperties;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.ingest.game.GameDocument;
import com.inplay.ingest.game.GameDocument.ScoreDocument;
import com.inplay.ingest.game.GameRepository;
import com.inplay.ingest.journal.SeasonJournalDocument;
import com.inplay.ingest.journal.SeasonJournalRepository;
import com.inplay.journal.season.JournalEntryGenerator;
import com.inplay.journal.season.NotionClient;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SeasonJournalServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-03T15:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate DATE = LocalDate.of(2026, 6, 4); // 15:00Z = 다음날 00:00 KST

    private final GameRepository gameRepo = Mockito.mock(GameRepository.class);
    private final SeasonJournalRepository journalRepo = Mockito.mock(SeasonJournalRepository.class);
    private final NotionClient notion = Mockito.mock(NotionClient.class);
    private final JournalEntryGenerator generator = new JournalEntryGenerator();

    private DefaultUserProperties user() {
        return new DefaultUserProperties("taeeho", KboTeam.HH, "Asia/Seoul", null, null, Map.of());
    }

    private NotionProperties notionProps(String apiKey, String dbId) {
        return new NotionProperties(apiKey, dbId, "2022-06-28");
    }

    private SeasonJournalService service(NotionProperties props) {
        return new SeasonJournalService(gameRepo, journalRepo, generator, notion, props, user(), CLOCK);
    }

    private GameDocument finalGame(String id, String home, String away, int h, int a) {
        return GameDocument.forNew(id, DATE, home, away, "FINAL", new ScoreDocument(h, a));
    }

    @Test
    void writesNotionPageAndMarkerForMyTeamFinalGame() {
        when(gameRepo.findByDateBetween(DATE, DATE)).thenReturn(List.of(finalGame("g1", "HH", "LG", 5, 3)));
        when(journalRepo.existsByUserIdAndSeasonAndGameId("taeeho", 2026, "g1")).thenReturn(false);
        when(notion.createPage(anyMap())).thenReturn(true);

        int written = service(notionProps("key", "db")).writeJournalsForToday();

        assertThat(written).isEqualTo(1);
        verify(notion).createPage(anyMap());
        verify(journalRepo).save(any(SeasonJournalDocument.class));
    }

    @Test
    void skipsWhenAlreadyRecorded() {
        when(gameRepo.findByDateBetween(DATE, DATE)).thenReturn(List.of(finalGame("g1", "HH", "LG", 5, 3)));
        when(journalRepo.existsByUserIdAndSeasonAndGameId("taeeho", 2026, "g1")).thenReturn(true);

        int written = service(notionProps("key", "db")).writeJournalsForToday();

        assertThat(written).isZero();
        verify(notion, never()).createPage(anyMap());
        verify(journalRepo, never()).save(any());
    }

    @Test
    void skipsNonMyTeamGame() {
        when(gameRepo.findByDateBetween(DATE, DATE)).thenReturn(List.of(finalGame("g1", "KIA", "SSG", 1, 0)));

        int written = service(notionProps("key", "db")).writeJournalsForToday();

        assertThat(written).isZero();
        verify(notion, never()).createPage(anyMap());
    }

    @Test
    void doesNotSaveWhenNotionFails() {
        when(gameRepo.findByDateBetween(DATE, DATE)).thenReturn(List.of(finalGame("g1", "HH", "LG", 5, 3)));
        when(journalRepo.existsByUserIdAndSeasonAndGameId("taeeho", 2026, "g1")).thenReturn(false);
        when(notion.createPage(anyMap())).thenReturn(false);

        int written = service(notionProps("key", "db")).writeJournalsForToday();

        assertThat(written).isZero();
        verify(journalRepo, never()).save(any());
    }

    @Test
    void skipsEntirelyWhenNotionNotConfigured() {
        int written = service(notionProps("", "")).writeJournalsForToday();

        assertThat(written).isZero();
        verify(gameRepo, never()).findByDateBetween(any(), any());
        verify(notion, never()).createPage(anyMap());
    }
}
