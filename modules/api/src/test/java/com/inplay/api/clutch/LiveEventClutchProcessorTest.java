package com.inplay.api.clutch;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.api.brief.DefaultUserProperties;
import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.decision.clutch.ClutchDetector;
import com.inplay.decision.clutch.ClutchFeatureBuilder;
import com.inplay.decision.clutch.ClutchNotifyPolicy;
import com.inplay.decision.clutch.ImportanceScorer;
import com.inplay.decision.clutch.MuteWindow;
import com.inplay.decision.clutch.RivalrySettings;
import com.inplay.decision.wpa.WpaAnnotator;
import com.inplay.inference.clutch.ClutchFeatures;
import com.inplay.inference.clutch.ClutchPredictor;
import com.inplay.notify.discord.DiscordWebhookClient;
import com.inplay.notify.discord.DiscordWebhookPayload;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiveEventClutchProcessorTest {

    private static final URI WEBHOOK = URI.create("https://discord.com/api/webhooks/x/y");
    private static final Instant NOW = Instant.parse("2026-05-12T12:34:56Z");

    @Mock ClutchPredictor predictor;
    @Mock DiscordWebhookClient discord;

    private final ClutchFeatureBuilder builder = new ClutchFeatureBuilder();
    private final ImportanceScorer scorer = new ImportanceScorer();
    private final WpaAnnotator annotator = new WpaAnnotator();

    private LiveEvent event(int inning, int outs, boolean[] runners, int home, int away,
                            String sourceEventId, OptionalDouble wpaAfter) {
        return new LiveEvent(NOW, new GameId("g1"), inning, InningHalf.BOTTOM,
                LiveEventType.PITCH, outs, runners, new Score(home, away),
                Optional.empty(), Optional.empty(), Optional.empty(),
                wpaAfter, "src", sourceEventId);
    }

    private Game game() {
        return new Game(new GameId("g1"), LocalDate.of(2026, 5, 12),
                KboTeam.HH, KboTeam.LG, GameStatus.LIVE, new Score(0, 0));
    }

    private DefaultUserProperties user(URI webhook) {
        return new DefaultUserProperties("taeeho", KboTeam.HH, "Asia/Seoul", webhook, null,
                Map.of(KboTeam.LG, 1.3));
    }

    private ClutchProperties props(boolean enabled) {
        return new ClutchProperties(enabled, 3.0, Duration.ofMinutes(5),
                Duration.ofMinutes(1), 0.7);
    }

    private LiveEventClutchProcessor processor(boolean enabled, ClutchPredictor pred) {
        var detector = new ClutchDetector(builder, pred, 0.7);
        var policy = new ClutchNotifyPolicy(3.0, Duration.ofMinutes(5), Duration.ofMinutes(1),
                MuteWindow.none(), Clock.fixed(NOW, ZoneOffset.UTC));
        var rivalry = new RivalrySettings(KboTeam.HH, Map.of(KboTeam.LG, 1.3));
        return new LiveEventClutchProcessor(annotator, detector, scorer, policy, rivalry,
                discord, user(WEBHOOK), props(enabled));
    }

    @Test
    void disabledShortCircuits() {
        var p = processor(false, predictor);
        boolean sent = p.process(event(8, 1, new boolean[]{true, true, true}, 3, 3, "e1", OptionalDouble.empty()),
                game());
        assertThat(sent).isFalse();
        Mockito.verifyNoInteractions(discord);
    }

    @Test
    void modelNotReadyDoesNotSend() {
        var p = processor(true, null);
        boolean sent = p.process(event(8, 1, new boolean[]{true, true, true}, 4, 3, "e1", OptionalDouble.empty()),
                game());
        assertThat(sent).isFalse();
        Mockito.verifyNoInteractions(discord);
    }

    @Test
    void clutchBelowThresholdNotSent() {
        Mockito.when(predictor.predict(ArgumentMatchers.any(ClutchFeatures.class))).thenReturn(0.4);
        var p = processor(true, predictor);
        boolean sent = p.process(event(3, 1, new boolean[3], 0, 0, "e1", OptionalDouble.empty()), game());
        assertThat(sent).isFalse();
        Mockito.verifyNoInteractions(discord);
    }

    @Test
    void clutchAboveThresholdSendsAndRecordsCooldown() {
        Mockito.when(predictor.predict(ArgumentMatchers.any(ClutchFeatures.class))).thenReturn(0.92);
        Mockito.when(discord.send(ArgumentMatchers.eq(WEBHOOK),
                        ArgumentMatchers.any(DiscordWebhookPayload.class))).thenReturn(true);

        var p = processor(true, predictor);
        boolean firstSent = p.process(
                event(9, 1, new boolean[]{true, true, true}, 4, 3, "e1", OptionalDouble.empty()),
                game());
        assertThat(firstSent).isTrue();
        Mockito.verify(discord).send(ArgumentMatchers.eq(WEBHOOK),
                ArgumentMatchers.any(DiscordWebhookPayload.class));

        // 같은 게임의 두 번째 이벤트는 cooldown으로 차단
        Mockito.clearInvocations(discord);
        boolean secondSent = p.process(
                event(9, 2, new boolean[]{true, true, true}, 5, 3, "e2", OptionalDouble.empty()),
                game());
        assertThat(secondSent).isFalse();
        Mockito.verifyNoInteractions(discord);
    }

    @Test
    void webhookFailureReturnsFalseAndStillAttemptsSend() {
        Mockito.when(predictor.predict(ArgumentMatchers.any(ClutchFeatures.class))).thenReturn(0.92);
        Mockito.when(discord.send(ArgumentMatchers.any(URI.class),
                        ArgumentMatchers.any(DiscordWebhookPayload.class))).thenReturn(false);

        var p = processor(true, predictor);
        boolean sent = p.process(
                event(9, 1, new boolean[]{true, true, true}, 4, 3, "e1", OptionalDouble.empty()),
                game());
        // policy를 실제 객체로 inject — webhook 실패 시 recordSent 미호출 검증은
        // ClutchNotifyPolicyTest에 위임. processor 레이어에서는 retry 가능성을 위해 send=false면 sent=false만.
        assertThat(sent).isFalse();
        Mockito.verify(discord).send(ArgumentMatchers.any(URI.class),
                ArgumentMatchers.any(DiscordWebhookPayload.class));
    }

    @Test
    void missingWebhookDoesNotSend() {
        Mockito.when(predictor.predict(ArgumentMatchers.any(ClutchFeatures.class))).thenReturn(0.92);
        var detector = new ClutchDetector(builder, predictor, 0.7);
        var policy = new ClutchNotifyPolicy(3.0, Duration.ofMinutes(5), Duration.ofMinutes(1),
                MuteWindow.none(), Clock.fixed(NOW, ZoneOffset.UTC));
        var rivalry = new RivalrySettings(KboTeam.HH, Map.of(KboTeam.LG, 1.3));
        var p = new LiveEventClutchProcessor(annotator, detector, scorer, policy, rivalry,
                discord, user(null), props(true));
        boolean sent = p.process(
                event(9, 1, new boolean[]{true, true, true}, 4, 3, "e1", OptionalDouble.empty()),
                game());
        assertThat(sent).isFalse();
        Mockito.verifyNoInteractions(discord);
    }
}
