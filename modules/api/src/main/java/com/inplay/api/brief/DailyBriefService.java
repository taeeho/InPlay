package com.inplay.api.brief;

import com.inplay.core.domain.game.Game;
import com.inplay.decision.brief.BriefFormatter;
import com.inplay.decision.brief.BriefGenerator;
import com.inplay.decision.brief.WinProbabilityBrief;
import com.inplay.ingest.game.GameDocument;
import com.inplay.ingest.game.GameMapper;
import com.inplay.ingest.game.GameRepository;
import com.inplay.notify.discord.DiscordWebhookClient;
import com.inplay.notify.discord.DiscordWebhookPayload;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 매일 아침 default user의 응원팀 경기 brief 발송. W7에서 user 컬렉션 loop로 확장.
 */
@Service
public class DailyBriefService {

    private static final Logger log = LoggerFactory.getLogger(DailyBriefService.class);

    private final GameRepository repository;
    private final BriefGenerator briefGenerator;
    private final DiscordWebhookClient discord;
    private final DefaultUserProperties user;
    private final Clock clock;

    public DailyBriefService(
            GameRepository repository,
            BriefGenerator briefGenerator,
            DiscordWebhookClient discord,
            DefaultUserProperties user,
            Clock clock) {
        this.repository = repository;
        this.briefGenerator = briefGenerator;
        this.discord = discord;
        this.user = user;
        this.clock = clock;
    }

    /** @return 발송한 brief 개수 (skip된 게임은 미포함). */
    public int sendBriefsForToday() {
        LocalDate today = LocalDate.now(clock);
        return sendBriefsFor(today);
    }

    int sendBriefsFor(LocalDate date) {
        URI webhook = user.discordWebhook();
        if (webhook == null) {
            log.warn("default user discord webhook not configured — skipping daily brief");
            return 0;
        }

        List<Game> todayGames = repository.findByDateBetween(date, date).stream()
                .map(GameMapper::toDomain)
                .toList();

        if (todayGames.isEmpty()) {
            log.info("no KBO games scheduled for {} — skipping daily brief", date);
            return 0;
        }

        List<Game> seasonHistory = repository.findByDateBetween(seasonStart(date), date.minusDays(1)).stream()
                .map(GameMapper::toDomain)
                .toList();

        int sent = 0;
        for (Game g : todayGames) {
            WinProbabilityBrief brief = briefGenerator.generate(g, seasonHistory, user.team());
            String content = BriefFormatter.format(brief);
            if (discord.send(webhook, DiscordWebhookPayload.of(content))) {
                sent++;
            }
        }
        log.info("daily brief sent={}/{} date={}", sent, todayGames.size(), date);
        return sent;
    }

    private static LocalDate seasonStart(LocalDate today) {
        // KBO 정규시즌 3월 하순 개막. 시즌 전체 history 의도 — 보수적으로 3/1.
        return LocalDate.of(today.getYear(), Month.MARCH, 1);
    }
}
