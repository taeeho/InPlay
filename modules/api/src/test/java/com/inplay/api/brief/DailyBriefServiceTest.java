package com.inplay.api.brief;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inplay.decision.brief.BriefGenerator;
import com.inplay.decision.brief.WinProbabilityBrief;
import com.inplay.decision.brief.WinProbabilityFeatureBuilder;
import com.inplay.ingest.game.GameDocument;
import com.inplay.ingest.game.GameRepository;
import com.inplay.notify.discord.DiscordWebhookClient;
import com.inplay.notify.discord.DiscordWebhookPayload;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyBriefServiceTest {

    @Mock GameRepository repository;
    @Mock DiscordWebhookClient discord;

    private static final URI HOOK = URI.create("https://discord.example/hook");
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 20);

    private DailyBriefService newService(URI webhook) {
        DefaultUserProperties user = new DefaultUserProperties("taeeho", com.inplay.core.domain.team.KboTeam.HH, "Asia/Seoul", webhook);
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        BriefGenerator gen = new BriefGenerator(new WinProbabilityFeatureBuilder(), null);
        return new DailyBriefService(repository, gen, discord, user, clock);
    }

    @Test
    void skipsWhenWebhookMissing() {
        DailyBriefService svc = newService(null);

        int sent = svc.sendBriefsForToday();

        assertThat(sent).isZero();
        verify(discord, never()).send(any(), any());
    }

    @Test
    void skipsWhenNoGames() {
        DailyBriefService svc = newService(HOOK);
        when(repository.findByDateBetween(TODAY, TODAY)).thenReturn(List.of());

        int sent = svc.sendBriefsForToday();

        assertThat(sent).isZero();
        verify(discord, never()).send(any(), any());
    }

    @Test
    void sendsBriefForEachTodayGame() {
        DailyBriefService svc = newService(HOOK);
        GameDocument g1 = GameDocument.forNew(
                "20260520HHLG", TODAY, "HH", "LG", "SCHEDULED",
                new GameDocument.ScoreDocument(0, 0));
        when(repository.findByDateBetween(TODAY, TODAY)).thenReturn(List.of(g1));
        when(repository.findByDateBetween(LocalDate.of(2026, 3, 1), TODAY.minusDays(1)))
                .thenReturn(List.of());
        when(discord.send(eq(HOOK), any(DiscordWebhookPayload.class))).thenReturn(true);

        int sent = svc.sendBriefsForToday();

        assertThat(sent).isEqualTo(1);
        verify(discord, times(1)).send(eq(HOOK), any(DiscordWebhookPayload.class));
    }

    @Test
    void countsOnlySuccessfulSends() {
        DailyBriefService svc = newService(HOOK);
        GameDocument g1 = GameDocument.forNew(
                "20260520HHLG", TODAY, "HH", "LG", "SCHEDULED",
                new GameDocument.ScoreDocument(0, 0));
        GameDocument g2 = GameDocument.forNew(
                "20260520KIASSG", TODAY, "KIA", "SSG", "SCHEDULED",
                new GameDocument.ScoreDocument(0, 0));
        when(repository.findByDateBetween(TODAY, TODAY)).thenReturn(List.of(g1, g2));
        when(repository.findByDateBetween(LocalDate.of(2026, 3, 1), TODAY.minusDays(1)))
                .thenReturn(List.of());
        when(discord.send(eq(HOOK), any(DiscordWebhookPayload.class)))
                .thenReturn(true, false);

        int sent = svc.sendBriefsForToday();

        assertThat(sent).isEqualTo(1);
        verify(discord, times(2)).send(eq(HOOK), any(DiscordWebhookPayload.class));
    }
}
