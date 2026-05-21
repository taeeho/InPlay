package com.inplay.api.clutch;

import com.inplay.api.brief.DefaultUserProperties;
import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.game.Game;
import com.inplay.decision.clutch.ClutchDetector;
import com.inplay.decision.clutch.ClutchNotifyPolicy;
import com.inplay.decision.clutch.ClutchVerdict;
import com.inplay.decision.clutch.ImportanceScore;
import com.inplay.decision.clutch.ImportanceScorer;
import com.inplay.decision.clutch.NotifyDecision;
import com.inplay.decision.clutch.RivalrySettings;
import com.inplay.decision.wpa.WpaAnnotator;
import com.inplay.notify.discord.ClutchPushFormatter;
import com.inplay.notify.discord.DiscordWebhookClient;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 라이브 이벤트 → WPA 스탬프 → clutch 판정 → importance 점수 → 정책 필터 → Discord 발사.
 *
 * <p>Pipeline 순서:
 * <ol>
 *   <li>{@link WpaAnnotator#annotate} 로 wpa_after 스탬프 (idempotent)</li>
 *   <li>game_id별 이전 이벤트(in-memory ConcurrentMap) lookup</li>
 *   <li>{@link ClutchDetector#detect} — verdict (isClutch=false면 skip)</li>
 *   <li>{@link ImportanceScorer#score} — 0~10 점수</li>
 *   <li>{@link ClutchNotifyPolicy#decide} — 4단계 필터</li>
 *   <li>shouldNotify이면 {@link DiscordWebhookClient#send} →
 *       <b>성공 시에만</b> {@link ClutchNotifyPolicy#recordSent}</li>
 * </ol>
 *
 * <p>prev event 캐시는 in-memory ConcurrentHashMap (베타 단일 프로세스 가정).
 * 재시작 시 game-level prev는 휘발 — 첫 이벤트가 we_before=0.5로 처리되어 사소한 부정확만.
 *
 * <p>Retry semantics 없음 (주의): {@link #process} 는 polling이 매 tick마다
 * <b>고유한 LiveEvent</b>(다른 sourceEventId)를 넘긴다고 가정. dedupe 차단은
 * {@code LiveEventIngestService} (Caffeine 60s, source_event_id) 가 1차로 담당.
 * 같은 LiveEvent를 두 번 넘기면 두 번째 호출에서 prev=직전 자신이라 wpa_change=0 →
 * BELOW_THRESHOLD로 자동 차단됨. 명시적인 retry는 caller가 별도 정책으로 처리.
 *
 * <p>Discord 429/5xx 같은 rate-limit 구분은 현재 X — DiscordWebhookClient는 single false 반환.
 * W7 다중 사용자 진입 시 retry-with-backoff 또는 rate-limit handling 별도 검토.
 *
 * <p>Game-level prev cache는 게임 종료 후에도 entry가 남음. 시즌 ~720 게임 × LiveEvent
 * 한 개라 누수는 무시 가능 수준. 운영 시 길어지면 game.status=FINAL 신호로 eviction.
 */
@Service
public class LiveEventClutchProcessor {

    private static final Logger log = LoggerFactory.getLogger(LiveEventClutchProcessor.class);

    private final WpaAnnotator wpaAnnotator;
    private final ClutchDetector detector;
    private final ImportanceScorer scorer;
    private final ClutchNotifyPolicy policy;
    private final RivalrySettings rivalrySettings;
    private final DiscordWebhookClient discord;
    private final DefaultUserProperties user;
    private final boolean enabled;

    private final ConcurrentMap<String, LiveEvent> previousByGame = new ConcurrentHashMap<>();

    public LiveEventClutchProcessor(
            WpaAnnotator wpaAnnotator,
            ClutchDetector detector,
            ImportanceScorer scorer,
            ClutchNotifyPolicy policy,
            RivalrySettings rivalrySettings,
            DiscordWebhookClient discord,
            DefaultUserProperties user,
            ClutchProperties props) {
        this.wpaAnnotator = Objects.requireNonNull(wpaAnnotator);
        this.detector = Objects.requireNonNull(detector);
        this.scorer = Objects.requireNonNull(scorer);
        this.policy = Objects.requireNonNull(policy);
        this.rivalrySettings = Objects.requireNonNull(rivalrySettings);
        this.discord = Objects.requireNonNull(discord);
        this.user = Objects.requireNonNull(user);
        this.enabled = Objects.requireNonNull(props).enabled();
    }

    /**
     * @return 발사된 알림이면 true, 어떤 단계에서든 suppress / 실패면 false.
     */
    public boolean process(LiveEvent rawEvent, Game game) {
        Objects.requireNonNull(rawEvent, "rawEvent required");
        Objects.requireNonNull(game, "game required");
        if (!enabled) {
            return false;
        }

        LiveEvent stamped = wpaAnnotator.annotate(rawEvent);
        String gameKey = stamped.gameId().value();
        LiveEvent prev = previousByGame.get(gameKey);
        previousByGame.put(gameKey, stamped);

        ClutchVerdict verdict = detector.detect(prev, stamped);
        if (!verdict.isClutch()) {
            return false;
        }

        ImportanceScore importance = scorer.score(verdict, game, prev, stamped, rivalrySettings);
        NotifyDecision decision = policy.decide(stamped, importance);
        if (!decision.shouldNotify()) {
            log.debug("clutch suppressed game={} reason={} importance={}",
                    gameKey, decision.suppressionReason(), importance.value());
            return false;
        }

        URI webhook = user.discordWebhook();
        if (webhook == null) {
            log.warn("clutch detected but default user webhook missing — game={}", gameKey);
            return false;
        }

        var payload = ClutchPushFormatter.format(prev, stamped, game, verdict, importance);
        boolean sent = discord.send(webhook, payload);
        if (sent) {
            policy.recordSent(stamped);
            log.info("clutch push sent game={} importance={} clutch={}",
                    gameKey, importance.value(), verdict.probability());
        } else {
            log.warn("clutch push send failed game={} — not recording cooldown to allow retry", gameKey);
        }
        return sent;
    }
}
