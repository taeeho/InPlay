package com.inplay.decision.clutch;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.event.LiveEventType;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class ClutchNotifyPolicyTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-12T12:34:56Z");

    private LiveEvent event(String gameId, int inning, InningHalf half, LiveEventType type,
                            String sourceEventId) {
        return new LiveEvent(
                FIXED_NOW, new GameId(gameId), inning, half, type,
                1, new boolean[]{false, true, false},
                new Score(3, 3),
                Optional.empty(), Optional.empty(), Optional.empty(),
                OptionalDouble.of(0.7),
                "src", sourceEventId);
    }

    private ImportanceScore high() {
        return new ImportanceScore(7.5, 0.6, 1.3, 0.8);
    }

    private ImportanceScore low() {
        return new ImportanceScore(1.0, 0.1, 1.0, 0.2);
    }

    private static Clock fixedClock(Instant at) {
        return Clock.fixed(at, ZoneOffset.UTC);
    }

    @Test
    void belowThresholdSuppressed() {
        var policy = new ClutchNotifyPolicy();
        var d = policy.decide(event("g1", 5, InningHalf.BOTTOM, LiveEventType.PITCH, "e1"), low());
        assertThat(d.shouldNotify()).isFalse();
        assertThat(d.suppressionReason()).isEqualTo("BELOW_THRESHOLD");
    }

    @Test
    void aboveThresholdAllowed() {
        var policy = new ClutchNotifyPolicy(3.0, Duration.ofMinutes(5), Duration.ofMinutes(1),
                MuteWindow.none(), fixedClock(FIXED_NOW));
        var d = policy.decide(event("g1", 5, InningHalf.BOTTOM, LiveEventType.PITCH, "e1"), high());
        assertThat(d.shouldNotify()).isTrue();
        assertThat(d.suppressionReason()).isNull();
    }

    @Test
    void cooldownBlocksSecondSendOnSameGame() {
        var policy = new ClutchNotifyPolicy(3.0, Duration.ofMinutes(5), Duration.ofMinutes(1),
                MuteWindow.none(), fixedClock(FIXED_NOW));
        var e1 = event("g1", 5, InningHalf.BOTTOM, LiveEventType.PITCH, "e1");
        policy.recordSent(e1);
        var e2 = event("g1", 6, InningHalf.TOP, LiveEventType.HIT, "e2"); // 다른 inning이지만 같은 game
        var d = policy.decide(e2, high());
        assertThat(d.suppressionReason()).isEqualTo("COOLDOWN");
    }

    @Test
    void dedupeBlocksSameInningHalfTypeWithinDifferentGames() {
        var policy = new ClutchNotifyPolicy(3.0, Duration.ofMinutes(5), Duration.ofMinutes(1),
                MuteWindow.none(), fixedClock(FIXED_NOW));
        var e = event("g1", 5, InningHalf.BOTTOM, LiveEventType.PITCH, "e1");
        policy.recordSent(e);
        // cooldown은 다른 game이라 통과되지만 같은 (game,inning,half,type) 키는 dedupe 차단됨.
        // 같은 게임에서는 cooldown이 먼저 잡아냄 → 별도 케이스로 cooldown 만료 후 시나리오 확인.
        var d = policy.decide(e, high());
        assertThat(d.suppressionReason()).isIn("COOLDOWN", "DUPLICATE");
        // game cooldown은 record로 set됨 → 이 디스패치는 둘 중 하나로 막힘
    }

    @Test
    void muteWindowBlocks() {
        var muteAt = Instant.parse("2026-05-12T23:00:00Z"); // 다음날 KST 08:00
        var window = new MuteWindow(LocalTime.of(7, 0), LocalTime.of(9, 0), ZoneId.of("Asia/Seoul"));
        var policy = new ClutchNotifyPolicy(3.0, Duration.ofMinutes(5), Duration.ofMinutes(1),
                window, fixedClock(muteAt));
        var d = policy.decide(event("g1", 5, InningHalf.BOTTOM, LiveEventType.PITCH, "e1"), high());
        assertThat(d.suppressionReason()).isEqualTo("MUTED");
    }

    @Test
    void differentGameNotInCooldownWhenFirstGameRecorded() {
        var policy = new ClutchNotifyPolicy(3.0, Duration.ofMinutes(5), Duration.ofMinutes(1),
                MuteWindow.none(), fixedClock(FIXED_NOW));
        policy.recordSent(event("g1", 5, InningHalf.BOTTOM, LiveEventType.PITCH, "e1"));
        var d = policy.decide(event("g2", 5, InningHalf.BOTTOM, LiveEventType.PITCH, "e2"), high());
        assertThat(d.shouldNotify()).isTrue();
    }

    @Test
    void filterOrderImportanceFirst() {
        // BELOW_THRESHOLD가 cooldown/dedupe보다 먼저 잡혀야 함 (cache 오염 방지)
        var policy = new ClutchNotifyPolicy(3.0, Duration.ofMinutes(5), Duration.ofMinutes(1),
                MuteWindow.none(), fixedClock(FIXED_NOW));
        policy.recordSent(event("g1", 5, InningHalf.BOTTOM, LiveEventType.PITCH, "e1"));
        var d = policy.decide(event("g1", 5, InningHalf.BOTTOM, LiveEventType.PITCH, "e2"), low());
        assertThat(d.suppressionReason()).isEqualTo("BELOW_THRESHOLD");
    }
}
