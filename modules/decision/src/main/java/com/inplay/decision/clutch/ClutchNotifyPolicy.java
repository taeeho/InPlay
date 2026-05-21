package com.inplay.decision.clutch;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.inplay.core.domain.event.LiveEvent;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Clutch 알림 4단계 필터 (PLAN.md §6):
 *   1. importance score &lt; threshold → BELOW_THRESHOLD
 *   2. 같은 game_id 최근 cooldown TTL 내 발사 이력 → COOLDOWN
 *   3. 같은 (game_id, inning, half, eventType) 최근 dedupe TTL 내 발사 → DUPLICATE
 *   4. mute window 시간대 → MUTED
 *
 * <p>state는 Caffeine in-memory cache. 재시작 시 휘발 (운영상 베타 5~10명 단일 프로세스 OK).
 * Persistent 알림 ledger는 W7에서 user별 분기와 함께 검토.
 *
 * <p>{@link #recordSent} 는 실제 webhook 전송 후 호출자가 명시적으로 부르는 게 책임 분리.
 * decide() 자체는 read-only 라 멱등 (테스트하기 쉽게).
 */
public final class ClutchNotifyPolicy {

    public static final double DEFAULT_IMPORTANCE_THRESHOLD = 3.0;
    public static final Duration DEFAULT_COOLDOWN = Duration.ofMinutes(5);
    public static final Duration DEFAULT_DEDUPE_TTL = Duration.ofMinutes(1);

    private final double importanceThreshold;
    private final Cache<String, Long> cooldownCache;
    private final Cache<String, Long> dedupeCache;
    private final MuteWindow muteWindow;
    private final Clock clock;

    public ClutchNotifyPolicy(double importanceThreshold,
                              Duration cooldown,
                              Duration dedupeTtl,
                              MuteWindow muteWindow,
                              Clock clock) {
        if (importanceThreshold < 0.0 || importanceThreshold > 10.0) {
            throw new IllegalArgumentException("importanceThreshold must be in [0,10], got " + importanceThreshold);
        }
        this.importanceThreshold = importanceThreshold;
        Objects.requireNonNull(cooldown, "cooldown required");
        Objects.requireNonNull(dedupeTtl, "dedupeTtl required");
        this.muteWindow = Objects.requireNonNull(muteWindow, "muteWindow required");
        this.clock = Objects.requireNonNull(clock, "clock required");
        Ticker ticker = () -> clock.instant().toEpochMilli() * 1_000_000L;
        this.cooldownCache = Caffeine.newBuilder().ticker(ticker).expireAfterWrite(cooldown).maximumSize(2_000).build();
        this.dedupeCache = Caffeine.newBuilder().ticker(ticker).expireAfterWrite(dedupeTtl).maximumSize(20_000).build();
    }

    public ClutchNotifyPolicy() {
        this(DEFAULT_IMPORTANCE_THRESHOLD, DEFAULT_COOLDOWN, DEFAULT_DEDUPE_TTL,
                MuteWindow.none(), Clock.systemUTC());
    }

    public NotifyDecision decide(LiveEvent event, ImportanceScore importance) {
        Objects.requireNonNull(event, "event required");
        Objects.requireNonNull(importance, "importance required");

        if (importance.value() < importanceThreshold) {
            return NotifyDecision.suppress(importance, "BELOW_THRESHOLD");
        }
        if (muteWindow.contains(clock.instant())) {
            return NotifyDecision.suppress(importance, "MUTED");
        }
        if (cooldownCache.getIfPresent(event.gameId().value()) != null) {
            return NotifyDecision.suppress(importance, "COOLDOWN");
        }
        if (dedupeCache.getIfPresent(dedupeKey(event)) != null) {
            return NotifyDecision.suppress(importance, "DUPLICATE");
        }
        return NotifyDecision.allow(importance);
    }

    public void recordSent(LiveEvent event) {
        Objects.requireNonNull(event, "event required");
        long now = clock.instant().toEpochMilli();
        cooldownCache.put(event.gameId().value(), now);
        dedupeCache.put(dedupeKey(event), now);
    }

    private static String dedupeKey(LiveEvent e) {
        return e.gameId().value() + "|" + e.inning() + "|" + e.half().name() + "|" + e.eventType().name();
    }
}
