# ADR-012: Clutch 알림 정책 — 4단계 필터 + retry semantics

- Status: Accepted
- Date: 2026-05-21

## Context
W4 In-game 결정적 순간 push의 운영 정책 결정. PLAN.md §6 4단계 필터를 코드 어디에 두고 어떤 key로 묶을지, 실패 시 어떻게 재시도할지 명확화 필요. decision 모듈은 단방향 DAG로 notify(webhook)에 의존 X — api가 조립.

## Decision

**4단계 필터** (`ClutchNotifyPolicy.decide`):
1. **importance < threshold** (기본 3.0/10) → `BELOW_THRESHOLD`
2. **mute window** (KST 기준, wrap-around 지원) → `MUTED`
3. **cooldown**: key=`game_id`, TTL 5분 → `COOLDOWN`
4. **dedupe**: key=`game_id|inning|half|eventType`, TTL 1분 → `DUPLICATE`

**Importance score** = `min(|ΔWE|/0.5, 1) × rivalry × min(leverage_proxy/1.5, 1) × 10`. rivalry: my_team match → 상대 weightFor (`max 1.0`), my_team 게임에 없음 → max(home,away) × 0.85 or floor 0.5.

**Retry semantics**: `decide()` read-only + `recordSent()` 명시 호출. webhook **성공 후에만** `recordSent` — 실패 시 cooldown 미적용, 다음 polling tick의 새 LiveEvent가 자연스럽게 재시도.

**조립 위치**: `api/clutch/LiveEventClutchProcessor` — `LiveEvent → WpaAnnotator → ClutchDetector → ImportanceScorer → ClutchNotifyPolicy → DiscordWebhookClient` 한 체인. prev event는 `ConcurrentHashMap<gameId, LiveEvent>` (베타 단일 프로세스).

**알려진 한계**:
- ConcurrentHashMap은 재시작 시 휘발 → 첫 이벤트가 we_before=0.5로 사소한 부정확
- Discord 429/5xx 구분 없이 일괄 fail → W7 다중 사용자에서 rate-limit handler 도입 검토
- game-level prev cache eviction 없음 → 시즌 ~720 entry 누적 (누수 무시 가능)

## Consequences
- decision/notify DAG 보존 (decision은 notify 의존 X)
- 같은 LiveEvent 두 번 process → prev=자신 → wpa_change=0 → BELOW_THRESHOLD 자동 차단 (idempotent by design)
- 면접 서사 가능: "4단계 필터 + 베타-사이즈 휘발 cache, W7에서 영속 ledger 확장"
