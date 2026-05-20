# inplay — KBO 통합 AI 동반시청 시스템 (전체 설계 plan)

> 이 문서는 프로젝트 전체 설계의 single source of truth. 다른 컴퓨터·다른 협업자가 컨텍스트를 잡을 때 가장 먼저 읽어야 할 문서.
> Codename: **inplay** ("in play" = 경기 진행 중. 시스템의 실시간 처리·결정적 순간 push 본질을 담음).
> Multi-tenant 베타로 본인(한화팬) + 친구 5~10명 사용.

---

## Context

**왜 이 프로젝트인가**
- 사용자(AX/AI 엔지니어 채용 준비 중인 풀스택 개발자, 한화 이글스 팬)가 진짜 매일 사용하면서 동시에 채용 어필 가능한 사이드 프로젝트.
- 단순 야구 통계 사이트(Statiz·KBO 공식 등 多)와 다르게, **한화팬으로서의 시청 경험을 4단계로 자동화**하는 게 차별화.
- 시청 전(brief) → 시청 중(실시간 push) → 시청 후(자동 회고) → 비시즌(다른 종목 전환).
- AI 핵심: 승률 예측 + 결정적 순간 감지(WPA 기반) + 투수 한계 예측 + 알림 정책 강화학습 (4개 모델 ensemble).

**사용 모드**
- **소규모 베타 (본인 + 친구 5~10명)**. 데이터·모델은 KBO 10개 구단 모두. 사용자별 응원팀·라이벌리·webhook 분리.
- 본인 default = 한화. 친구가 LG팬이면 LG 시점 brief/push.
- 인증은 간단 API key (Spring Security filter). OAuth·정식 회원가입은 v2.

**무엇이 아닌가 (스코프 컷)**
- 매크로·자동매매·자동결제 X (이전 ticketer 폐기 학습)
- MLB는 다루지 않음 — KBO 한정
- 공개 SaaS X (정식 가입·결제·이용약관 없음)
- K8s는 W8 골격만, 운영은 Podman로 충분

**합법 안전선 (불변)**
1. 데이터 출처: KBO 공식 API/사이트, Statiz, 네이버 스포츠 라이브 (분당 1회·경기 중 30초, robots.txt 준수, User-Agent 명시)
2. 매크로/자동 X — 정보·예측·알림·자동 회고까지만
3. 본인 사용용 — 대량 데이터 재배포 X
4. 새 외부 source 추가 시 ToS·robots 확인 + ADR 작성 게이트

---

## 0. 메타

| 항목 | 값 |
|---|---|
| Codename | `inplay` |
| 작업 경로 | `~/Documents/inplay/` (macOS), repo root |
| Stack | Java 21 LTS + Spring Boot 3.3 + MongoDB 7 + ONNX Runtime Java + Podman |
| 학습 환경 | Python(PyTorch + LightGBM) → ONNX export → JVM 추론 |
| 알림 채널 | Discord webhook |
| UI | Notion (자동 일지) + Discord (실시간 알림) + Thymeleaf/HTMX 미니 대시보드 (ADR-010, 2026-05-20 추가) |
| 응원팀 | 본인 default 한화 (사용자별 설정 가능, 데이터·모델은 10구단 모두) |
| 사용자 모드 | 소규모 베타 5~10명 (본인 + 친구), 간단 API key 인증 |
| 배포 모드 | local-first. `.env` Supabase/Vercel 값 있으면 사용, 없으면 로컬 fallback |
| 기간 | 6주 core + 2주 buffer |
| Repo | https://github.com/taeeho/InPlay |

---

## 1. 시스템 아키텍처 — Spring Multi-Module 8개

```
inplay/
├── modules/
│   ├── core/              # 공통 도메인·DTO·예외·시간 유틸
│   ├── collector/         # KBO·Statiz·Naver polling, robots.txt 가드
│   ├── ingest/            # MongoDB 적재, 디바운싱, timeseries 라우팅
│   ├── ml-inference/      # ONNX Runtime 래퍼, 모델 레지스트리, hot-swap
│   ├── decision/          # WPA·위기 감지·알림 정책 엔진(bandit)
│   ├── notify/            # Discord webhook, cooldown, dedupe
│   ├── journal/           # Notion API, 시즌 일지 생성, 뉴스 요약
│   └── api/               # Spring Boot main, REST(/admin), scheduler 진입
├── python/trainer/        # PyTorch·LightGBM 학습, ONNX export
├── infra/
│   ├── compose/           # podman-compose (mongo, ollama)
│   └── k8s/               # 골격만 (W8)
├── docs/
│   ├── PLAN.md            # 본 문서
│   └── adr/               # ADR-001~ (chore/w1-ops 브랜치에서 시드)
├── scripts/podman-gradle.sh
├── Makefile
├── HANDOFF.md             # 진행 상태 + 다음 액션
├── CLAUDE.md              # Claude Code 작업 룰
└── README.md              # 빌드·실행 가이드
```

**의존 DAG**: `api` → `decision` + `journal` + `notify` → `ingest` + `ml-inference` → `collector` → `core`. 단방향.

**컨테이너**: `mongo:7`(필수, replica set rs0), `ollama`(선택, 비시즌 뉴스 요약), Spring Boot api는 W8에서 컨테이너화. Python trainer는 노트북 직접 실행 (산출물 ONNX만 commit).

---

## 2. 데이터 흐름

```
[KBO API] [Statiz] [Naver Live] [Weather] [News RSS]
    \        |          |          /          /
     \_______|__________|_________/__________/
                       v
            [collector] (rate limit + robots + UA)
                       v
            [ingest] (debounce + dedupe + route)
                       v
       +---------------+-----------------+
       v               v                 v
   game/player    live_event(TS)    pitch_log(TS)
       |               |                 |
       +-------+-------+--------+--------+
               v                v
       [decision: WPA]   [ml-inference: ONNX]
               \                /
                v              v
            [notify policy engine]
            /          \           \
           v            v           v
     Discord push  alert_event  season_journal
                    (log)            |
                                     v
                                Notion API
```

---

## 3. MongoDB 스키마 (11 컬렉션)

| 컬렉션 | 유형 | 키 인덱스 |
|---|---|---|
| `user` | 일반 | `user_id` unique, `api_key_hash` unique |
| `team` | 일반 | `team_code` unique |
| `player` | 일반 | `player_id` unique, `team_code` |
| `game` | 일반 | `game_id` unique, `(date, home_team, away_team)` |
| `pre_game_brief` | 일반 | `(user_id, game_id)` unique |
| `live_event` | **timeseries** (sec) | meta: `game_id` (공유) |
| `pitch_log` | **timeseries** (sec) | meta: `game_id` (공유) |
| `pitcher_stat_daily` | 일반 | `(player_id, date)` unique |
| `model_snapshot` | 일반 | `(model_name, version)` unique |
| `alert_event` | **timeseries** (sec) | meta: `user_id` |
| `season_journal` | 일반 | `(user_id, season, game_id)` unique |

**user 스키마 핵심**:
```js
{
  user_id: "u_taeeho",
  name: "taeeho",
  api_key_hash: "sha256(...)",
  my_team: "HH",                        // KBO 10팀 코드
  rivalry_weights: { LG: 1.3, KIA: 1.25, LT: 1.2 },
  discord_webhook_url: "https://discord.com/api/webhooks/...",
  mute_window: { start: "08:00", end: "10:00", timezone: "Asia/Seoul" },
  created_at: ISODate(...)
}
```

**공유 vs 사용자별 분리**:
- **공유** (모든 사용자 공통): `team`, `player`, `game`, `live_event`, `pitch_log`, `pitcher_stat_daily`, `model_snapshot`
- **사용자별**: `user`, `pre_game_brief`, `alert_event`, `season_journal`

**timeseries** 컬렉션은 자동 압축 + TTL(시즌 + 한 달).

**핵심 스키마 — `live_event`**
```js
{
  event_ts: ISODate("2026-05-12T19:34:21Z"),
  meta: { game_id: "20260512HHLG", inning: 5, half: "bottom" },
  event_type: "pitch",        // pitch | hit | out | sub | end_inning
  pitch: { type: "FF", speed_kmh: 147, result: "ball" },
  batter_id, pitcher_id,
  outs: 2, runners: [1,1,1],   // 만루
  score: { home: 3, away: 4 },
  wpa_after: 0.412,
  source: "naver_live",
  source_event_id: "naver:abc123"  // 디바운싱 키
}
```

---

## 4. 모델 라인업 — 4개 ensemble (W7 RL은 cut 가능)

| 단계 | 모델 | 학습 → 서빙 |
|---|---|---|
| **승률 예측** (Pre-game) | LightGBM 베이스라인 → LSTM (W5 업그레이드, 시간 부족시 cut) | Python → ONNX → Java |
| **결정적 순간 감지** (In-game) | LightGBM classifier (WPA 변동 + 컨텍스트) | Python → ONNX → Java |
| **투수 한계 예측** | LSTM (누적 구수·구속·타순 회전) | Python → ONNX → Java |
| **알림 정책** | Contextual Bandit (LinUCB), W7 도입 (cut 가능) | Java native |

**parity check 필수** — Python 추론 vs Java ONNX 추론 결과 1e-4 이내 일치 (회귀 테스트).
**모델 버전 관리**: `model_snapshot` 컬렉션 + `modules/ml-inference/src/main/resources/models/v{n}/` + sha256.

---

## 5. 실시간 처리 전략

| 항목 | 결정 | 근거 |
|---|---|---|
| 동시성 모델 | **Spring MVC + Java 21 virtual thread** | 동시 경기 ≤ 5, 분당 ≤ 600 이벤트. WebFlux 오버엔지니어링 |
| polling 주기 | 경기 중 30초, 비경기 5분, 종료 즉시 stop | 매너 + 효율 |
| 디바운싱 | `source_event_id` Caffeine cache 60초 TTL + MongoDB unique index 백업 | 이중 안전망 |
| backpressure | scheduler thread pool 4, 큐 가득 차면 polling 주기 자동 ↑ | 외부 차단 회피 |
| robots.txt 가드 | `collector` 시작 시 fetch·캐시 24h, 위반 detect 시 자동 stop + Discord 경고 | 합법 안전선 |

---

## 6. Discord 알림 정책 (4단계 필터)

1. **importance score** — `wpa_change × rivalry_weight × leverage_index` (0~10)
2. **cooldown** — 동일 game_id 5분 내 중복 차단
3. **batching** — 동일 이닝·동일 카테고리 push 1건으로 합치기
4. **mute window** — 사용자 설정 시간대(통근 등) silent

W7에서 LinUCB로 본인 응답 패턴에 맞춰 step1 weight 학습 (cut 가능).

**메시지 템플릿**:
```
[5회말 만루 위기]
류현진 87구 / ERA 1.85 / 다음 타자 vs 좌타 .312
교체 가능성 73% — 지금 보세요
중계: KBSN | 4-3 한화 리드
```

---

## 7. 비시즌 전환 — `SportAdapter` 추상화

```java
public interface SportAdapter {
  String code();                              // "KBO" | "KLEAGUE" | "KBL" | "VLEAGUE"
  Season currentSeason(LocalDate today);
  List<Game> fetchSchedule(LocalDate from, LocalDate to);
  Stream<LiveEvent> pollLive(String gameId);
  PreGameBrief brief(String gameId);
}
```

`decision`/`notify`/`journal` 모듈은 adapter에만 의존. 11~3월 KBO 빈 schedule → 다른 종목 자동 활성. W6 K리그 stub만.

---

## 8. 6+2 주차 마일스톤

### W1 — 골격·수집 파이프라인
- multi-module Gradle 8개 + MongoDB 7 podman compose 기동
- KBO 일정·결과 수집 batch
- **Gate**: `make test` 통과, 7일 경기 100% 수집, robots 위반 0건

### W2 — Pre-game 브리핑 + 베이스라인 ML
- LightGBM 승률 모델 (Python) → ONNX export → Java 통합
- 매일 08:00 Discord brief
- **Gate**: holdout 50경기 accuracy ≥ 0.58, 7일 연속 brief 수신

### W3 — 실시간 이벤트 + WPA
- 네이버 라이브 polling (30초), `live_event` timeseries 적재
- WPA 계산 엔진 (rule-based, run expectancy table)
- **Gate**: 한 경기 풀 수집 누락 < 2%, WPA 합 ±0.05

### W4 — In-game 결정적 순간 감지 + Push
- WPA + 투수 누적구수 + 위기 컨텍스트 → "위기" classifier (LightGBM)
- Discord push (cooldown 5분, dedupe, importance score)
- **Gate**: 5경기 ride-along, 본인 평가 precision ≥ 0.7

### W5 — DL 모델 + 투수 한계 예측
- LSTM 투수 한계 모델 (누적 구수·구속·타순)
- W2 LightGBM 승률 → LSTM 업그레이드 (시간 부족시 cut)
- **Gate**: 투수 한계 AUC ≥ 0.72, 추론 latency p95 < 100ms

### W6 — Post-game 시즌 일지 + User 도메인
- 경기 종료 후 Notion API로 `season_journal` 자동 생성 (사용자별)
- **User 도메인** + API key 인증 (Spring Security filter)
- `SportAdapter` 추상화 + K리그 stub
- **Gate**: 본인 5경기 일지 자동 생성 100%, API key 발급/검증 통과

### W7 — 사용자별 설정 + 알림 정책 분리
- 사용자별 my_team/rivalry_weights/mute_window/webhook 설정 API
- 알림 정책 엔진을 사용자별 분기
- 친구 모집 준비
- **Gate**: 본인 + 가짜 LG팬·KIA팬 시뮬레이션 → 각자 시점 brief/push 정상

### W8 — 베타 onboarding + 데모
- 친구 5명 onboarding (user 등록 + API key/webhook 안내)
- 한화 트레이드/FA 뉴스 RSS + LLM 요약
- 데모 영상 3분, 아키텍처 PDF, README 정리
- Spring Boot api 컨테이너화

> **W7 RL bandit cut**: 사용자당 데이터 부족 + multi-user 우선. v2에서 사용자 통합 데이터로 재시도.

---

## 9. 검증 게이트 표

| 주 | 자동 명령 | 메트릭 임계 | 실사용 확인 |
|---|---|---|---|
| W1 | `make test` (collector) | 7일 경기 100% | mongo-express에서 game 확인 |
| W2 | `make test` (ml-inference) | accuracy ≥ 0.58 | 7일 연속 Discord brief |
| W3 | `make test` (ingest) | 누락 < 2%, WPA 합 ±0.05 | 1경기 풀 ride-along |
| W4 | `make test` (decision) | F1 ≥ 0.7 | 5경기 본인 평가 precision ≥ 0.7 |
| W5 | `make test` (ml-inference) | AUC ≥ 0.72, p95 < 100ms | 1주 안정 |
| W6 | `make test` (journal + api) | 일지 100% + API key 인증 | Notion 5건, 본인 가입 |
| W7 | `make test` (decision + api) | 사용자별 분기 100% | 가짜 LG팬·KIA팬 시뮬 |
| W8 | `make build` + podman build | 컨테이너 부팅 < 30s | 친구 5명 onboarding + 데모 영상 |

---

## 10. 위험·컷라인

| 위험 | 확률 | 대응 |
|---|---|---|
| 네이버 라이브 스키마 변경·차단 | 중 | KBO 공식 API 우선, Naver fallback. 차단 시 polling 5분으로 완화 |
| LSTM 학습 시간 부족 | 중 | **W5 LSTM 승률 cut**, LightGBM 유지 |
| 강화학습 데이터 부족 (1인 사용) | 높 | **W7 RL cut**, rule-based 알림 유지 |
| Notion API rate limit | 저 | 일지 game 종료 후 1건/30분 |
| ONNX parity 불일치 | 저 | W2 끝에 parity 테스트 강제 |

**우선순위**: W1~W4 (수집·brief·실시간 push) > W5 (DL) > W6 (일지) > W7 (RL) > W8 (운영). **W4까지 완성 시 채용 어필 최소 임계 통과**.

---

## 11. 채용 어필 4축 매핑

| 산출물 | 임팩트 | 프로덕션 | 시스템 통합 | 도메인 |
|---|:---:|:---:|:---:|:---:|
| Spring Multi-Module + Java 21 virtual thread | | O | O | |
| MongoDB timeseries + Change Stream | | O | O | |
| ONNX Runtime Java 추론 통합 + parity test | | O | O | |
| WPA 모델링 + 결정적 순간 감지 | O | | | O |
| Discord 4단계 알림 정책 + bandit | O | | O | |
| 자동 시즌 일지 (Notion API) | O | | O | O |
| 비시즌 SportAdapter 추상화 | | O | | O |
| 본인 시즌 75경기 push 응답률 측정 | O | | | O |
| **실사용자 N명 베타 운영 (multi-tenant)** | O | O | O | |

**면접 30초 핵심 서사**: "KBO 10구단 통합 시청 어시스턴트. KBO 공식 + Statiz + 네이버 라이브 멀티 source를 Spring Boot로 통합, MongoDB timeseries에 적재, LightGBM/LSTM 3개 모델 ensemble로 결정적 순간 감지 + ONNX로 Python 학습/JVM 추론 분리. 사용자별 응원팀·webhook 분리한 multi-tenant 베타로 본인 + 친구 N명이 시즌 내내 사용 중."

---

## 12. Notion 정리 구조

```
📒 InPlay (top page)
├── 0. 개요 & 채용 어필 4축
├── 1. 아키텍처
├── 2. DB 모델링 — DB: collections
├── 3. 모델 카드 — DB
├── 4. 주차 일지 — DB
├── 5. 실사용 로그 — DB
├── 6. 결정 로그 ADR — DB
└── 7. 시즌 자동 일지 — DB
```

DB 6개는 Notion UI에서 수동 생성 (자세한 칼럼 정의는 페이지 본문 참조).

---

## 13. 빌드 환경 (불변)

- 모든 gradle 빌드는 podman 컨테이너 (`docker.io/gradle:8.10-jdk21`)
- Host JDK 버전 무관 (개발 host Java 25, 컨테이너 JDK 21 고정)
- 사용 명령: `make compile`, `make test`, `make build`, `./scripts/podman-gradle.sh <task>`
- Host에 직접 `gradle` 또는 `./gradlew` 호출 금지

---

## 14. 핵심 의사결정 (재확인용)

- **WebFlux 미채택** — Spring MVC + Java 21 virtual thread (동시 경기 ≤ 5)
- **ONNX 경유** — Python 학습 + JVM 추론 분리. parity test 1e-4 이내
- **응원팀·라이벌리 가중치 데이터화** — 코드 하드코딩 X, user 컬렉션 저장
- **데이터·모델 KBO 10구단 공유, 사용자 시점만 분리** (multi-tenant)
- **W4까지 완성이 채용 어필 최소 임계** (W5~W8은 buffer)
- **W7 RL bandit cut** (사용자당 데이터 부족, v2)
- **비시즌 `SportAdapter` 추상화** — K리그/KBL/V리그 자동 전환 (W6 stub만)
- **WebFlux/GraphQL/Kafka 도입 금지** — 현 규모 불필요
- **학습 코드 Java 포팅 금지** — ONNX 경계 유지
- **Discord push 자동 X** — 사용자가 직접 push (브랜치 push 룰)
