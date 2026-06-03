package com.inplay.api.journal;

import com.inplay.api.brief.DefaultUserProperties;
import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.ingest.game.GameMapper;
import com.inplay.ingest.game.GameRepository;
import com.inplay.ingest.journal.SeasonJournalDocument;
import com.inplay.ingest.journal.SeasonJournalRepository;
import com.inplay.journal.season.JournalEntryGenerator;
import com.inplay.journal.season.NotionClient;
import com.inplay.journal.season.NotionJournalPage;
import com.inplay.journal.season.SeasonJournalEntry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 종료된 경기를 default user 시점으로 Notion 시즌 일지에 기록. W7에서 user 컬렉션 loop로 확장.
 *
 * <p>중복 방지: {@code exists → Notion POST → save}. <b>단일 인스턴스 MVP 전제</b>(ADR-014) —
 * 다중 인스턴스/동시 실행이면 Notion 중복 page 가능. Mongo unique index는 저장만 막는다.
 */
@Service
public class SeasonJournalService {

    private static final Logger log = LoggerFactory.getLogger(SeasonJournalService.class);

    private final GameRepository gameRepository;
    private final SeasonJournalRepository journalRepository;
    private final JournalEntryGenerator generator;
    private final NotionClient notionClient;
    private final NotionProperties notion;
    private final DefaultUserProperties user;
    private final Clock clock;

    public SeasonJournalService(
            GameRepository gameRepository,
            SeasonJournalRepository journalRepository,
            JournalEntryGenerator generator,
            NotionClient notionClient,
            NotionProperties notion,
            DefaultUserProperties user,
            Clock clock) {
        this.gameRepository = gameRepository;
        this.journalRepository = journalRepository;
        this.generator = generator;
        this.notionClient = notionClient;
        this.notion = notion;
        this.user = user;
        this.clock = clock;
    }

    /** @return Notion에 새로 생성한 일지 건수. */
    public int writeJournalsForToday() {
        return writeJournalsFor(LocalDate.now(clock));
    }

    int writeJournalsFor(LocalDate date) {
        if (isBlank(notion.apiKey()) || isBlank(notion.journalDatabaseId())) {
            log.warn("notion api-key/journal-database-id 미설정 — 시즌 일지 생성 skip");
            return 0;
        }

        String userId = (user.name() == null || user.name().isBlank()) ? "default" : user.name();
        KboTeam myTeam = user.team();

        List<Game> games = gameRepository.findByDateBetween(date, date).stream()
                .map(GameMapper::toDomain)
                .toList();

        int written = 0;
        for (Game game : games) {
            Optional<SeasonJournalEntry> opt = generator.generate(game, myTeam, userId);
            if (opt.isEmpty()) {
                continue;
            }
            SeasonJournalEntry entry = opt.get();
            if (journalRepository.existsByUserIdAndSeasonAndGameId(userId, entry.season(), entry.gameId())) {
                continue;
            }
            boolean ok = notionClient.createPage(NotionJournalPage.requestBody(entry, notion.journalDatabaseId()));
            if (!ok) {
                continue;
            }
            try {
                journalRepository.save(SeasonJournalDocument.forNew(
                        userId, entry.season(), entry.gameId(), Instant.now(clock)));
            } catch (DuplicateKeyException e) {
                // 단일 인스턴스 MVP 전제. 동시 실행/재시도로 이미 기록됐으면 무시(ADR-014).
                log.warn("season_journal 중복 저장 무시 (user={} season={} game={})",
                        userId, entry.season(), entry.gameId());
            }
            written++;
        }
        log.info("season journal written={} date={}", written, date);
        return written;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
