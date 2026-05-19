# inplay — Handoff

> 다른 컴퓨터·새 협업자가 이 프로젝트를 이어서 작업할 때 가장 먼저 읽는 문서.
> **현재 상태**·**다음 액션**·**환경 셋업** 한눈에.

---

## TL;DR

- KBO 통합 AI 동반시청 시스템 (한화팬용 default, multi-tenant 베타)
- Spring Boot 3.3 + Java 21 + MongoDB 7 + ONNX Runtime + Discord webhook + Notion API
- **모든 빌드는 podman 컨테이너 안** (host JDK 무관)
- 6주 core + 2주 buffer 마일스톤
- 전체 설계: [`docs/PLAN.md`](docs/PLAN.md)
- Claude Code 작업 룰: [`CLAUDE.md`](CLAUDE.md)

---

## 현재 상태 (마지막 갱신: 2026-05-19)

### 완료 (W0 — 골격)
- [x] Plan 승인 + 메모리 등록
- [x] Notion 7페이지 콘텐츠 시드 (https://www.notion.so/InPlay-35dbc507d1ef80abac81cb5318e96c78)
- [x] Gradle multi-module 8개 골격 (core/collector/ingest/ml-inference/decision/notify/journal/api)
- [x] Spring Boot 3.3 + Java 21 + MongoDB 7 podman compose
- [x] ONNX Runtime Java scaffolding
- [x] CLAUDE.md / README.md / .env.example / .gitignore
- [x] 빌드 환경 컨테이너화 (Makefile + scripts/podman-gradle.sh)
- [x] 사용자 준비물: GitHub repo · Discord webhook · Notion 페이지 · 라이벌리 가중치
- [x] HANDOFF.md + docs/PLAN.md (본 문서)

### 완료 (W1 — 코드 골격)
- [x] **feat/w1-core** — modules/core/** 도메인 record + 54 invariant tests (머지 `0936d4c`)
- [x] **chore/w1-ops** — mongo-init scripts + CI workflow + ADR-001~007 (머지 `7fa2b34`)
- [x] **feat/w1-collector** — RestClient + RobotsGuard + RateLimiter + KboScheduleClient/Parser/Service + 25 tests (머지 `f1b56aa`)
- [x] **feat/w1-ingest** — GameDocument + Repository + Mapper + IngestService + 9 unit / 4 integration tests (머지 `461548f`)
- [x] **ADR-008** Accepted — KBO 공식 `/Schedule/Schedule.aspx` 채택. Statiz/Naver/Daum은 robots/ToS 차단으로 폐기 (`docs/adr/notes/adr-008-source-survey.md`).
- [x] **ADR-009** Accepted — Playwright headless 회색지대 운영 조건 (UA 명시, 분당 1회, `/ws/` AJAX `route.abort`, 베타 5~10명 한정).

### 진행 중 (W1 마무리)
- [ ] **collector Playwright 통합 PR** — 현재 jsoup-only → headless 하이브리드. `ScheduleSource` 인터페이스 분리, application.yml URL 주입, integration test 환경(podman socket).
- [ ] **W1 게이트 재해석**: "7일 경기 100% 수집"은 headless 통합 후로 미룸. 코드 게이트(`make test`)는 통과(79 tests).

### ⚠️ W1 결정적 발견 — 합법 자동 수집 source 부재
사용자 + Claude Code의 사람 확인(2026-05-19) 결과 한국 야구 일정 4개 후보(Statiz/Naver/Daum/KBO) 모두 ADR-005 보수 원칙(robots 자동 봇 직접 fetch)으로는 일정 데이터 수집 **불가**. 상세는 `docs/adr/notes/adr-008-source-survey.md`.

ADR-009로 headless 회색지대 운영 결정 — KBO `/Schedule/Schedule.aspx`만 Playwright로 SSR된 DOM 파싱, `/ws/`는 직접 호출 X. 베타 5~10명 한정, 데이터 재배포 X, KBO ToS에 자동 렌더링 금지 명시 발견 시 즉시 중단.

후속 옵션: 사용자가 KBO 공식 측 베타 사용 문의 (정식 협력).

### 대기 (사용자 1회 작업)
- [ ] Notion DB 6개 수동 생성 (Notion UI에서 `/database` Inline. 칼럼 정의는 각 페이지 본문 참조). W6 전까지만 OK.
- [ ] (W7~W8 시점) 베타 친구 5~10명 명단 (이름·응원팀·Discord webhook URL)
- [ ] (선택) KBO 공식 측 베타 사용 문의 메일

---

## 마일스톤 체크리스트 (W1~W8)

`docs/PLAN.md` §8 / §9 참조. 게이트 통과 시 체크.

### W1 — 골격·수집 파이프라인
- [x] core 도메인 record + 54 invariant 테스트
- [x] collector KBO HTTP client + robots.txt 가드 + RateLimiter (jsoup, headless 통합은 후속)
- [x] ingest MongoDB Document + Repository + Testcontainers(integration tag)
- [x] infra mongo-init script + CI workflow + ADR-001~007
- [x] ADR-008/009 — source 결정 + headless 회색지대 운영
- [x] **코드 게이트**: `make test` 통과 (79 tests, robots 위반 0)
- [ ] **데이터 게이트**: 7일 경기 100% 수집 — collector Playwright 통합 PR 이후로 미룸

### W2 — Pre-game 브리핑 + 베이스라인 ML
- [ ] LightGBM 승률 모델 (Python) + ONNX export
- [ ] Java ONNX Runtime 통합 + parity test
- [ ] 매일 08:00 Discord brief 발송
- [ ] **Gate**: holdout 50경기 accuracy ≥ 0.58, 7일 연속 brief 수신

### W3 — 실시간 이벤트 + WPA
- [ ] 네이버 라이브 polling (30초), live_event timeseries 적재
- [ ] WPA 계산 엔진 (rule-based, run expectancy table)
- [ ] 디바운싱 (Caffeine cache + Mongo unique index)
- [ ] **Gate**: 한 경기 풀 수집 누락 < 2%, WPA 합 ±0.05

### W4 — In-game 결정적 순간 감지 + Push
- [ ] WPA + 투수 누적구수 + 위기 컨텍스트 → classifier (LightGBM)
- [ ] Discord push (cooldown 5분, dedupe, importance score)
- [ ] **Gate**: 5경기 ride-along, 본인 평가 precision ≥ 0.7
- [ ] 🎯 **W4 완성 = 채용 어필 최소 임계 통과**

### W5 — DL 모델 + 투수 한계 예측
- [ ] LSTM 투수 한계 모델 (PyTorch → ONNX → Java)
- [ ] (선택) W2 LightGBM 승률 → LSTM 업그레이드
- [ ] **Gate**: 투수 한계 AUC ≥ 0.72, 추론 latency p95 < 100ms

### W6 — Post-game 시즌 일지 + User 도메인
- [ ] Notion API 자동 일지 (`season_journal`, 사용자별)
- [ ] User 도메인 + API key 인증 (Spring Security filter)
- [ ] SportAdapter 추상화 + K리그 stub
- [ ] **Gate**: 본인 5경기 일지 100%, API key 발급/검증 통과

### W7 — 사용자별 설정 + 알림 정책 분리
- [ ] 사용자별 my_team/rivalry_weights/mute_window/webhook 설정 API
- [ ] 알림 정책 엔진 사용자별 분기
- [ ] (cut 가능) LinUCB bandit
- [ ] **Gate**: 본인 + 가짜 LG팬·KIA팬 시뮬 → 각자 시점 brief/push 정상

### W8 — 베타 onboarding + 데모
- [ ] 친구 5명 onboarding (user 등록 + API key/webhook 안내)
- [ ] 한화 트레이드/FA 뉴스 RSS + LLM 요약
- [ ] 데모 영상 3분, 아키텍처 PDF, README 정리
- [ ] Spring Boot api 컨테이너화
- [ ] **Gate**: 컨테이너 부팅 < 30s, 친구 5명 onboarding 완료

---

## 현재 브랜치·worktree

```bash
git worktree list
# /Users/hataeho/Documents/inplay            [main]                     ← 작업 기준
# /Users/hataeho/Documents/inplay-w1-coll    [feat/w1-collector]        ← 머지 완료, 후속 정리 가능
# /Users/hataeho/Documents/inplay-w1-core    [feat/w1-core]             ← 머지 완료
# /Users/hataeho/Documents/inplay-w1-ingest  [feat/w1-ingest]           ← 머지 완료
# /Users/hataeho/Documents/inplay-w1-ops     [chore/w1-ops]             ← 머지 완료
```

W1 브랜치 4개 모두 main에 머지 완료. 후속 작업(Playwright 통합 등)은 새 브랜치(`feat/w1-collector-headless` 등) 따서 진행.

병렬 작업 룰:
- 각 브랜치는 자기 모듈 디렉토리만 수정
- `application.yml`은 한 명만 수정
- 루트 파일 (`build.gradle.kts`/`Makefile`/`README`/`CLAUDE.md`) 수정 시 별도 PR
- 커밋 메시지는 사전 텍스트로 제시 후 사용자 승인 (자동 커밋 X)

---

## 다른 컴퓨터에서 이어 작업하기

### 1. Clone + 환경 셋업
```bash
git clone https://github.com/taeeho/InPlay.git inplay
cd inplay

# Podman 설치 (macOS)
brew install podman
podman machine init
podman machine start

# 환경 변수
cp .env.example .env
# .env 편집 (Discord webhook URL, Notion key 등)
```

### 2. 빌드 sanity check
```bash
make compile    # 컨테이너 안에서 컴파일 (첫 빌드 시 의존성 다운로드, 시간 좀 걸림)
make test       # 전 모듈 테스트
make help       # 다른 명령 보기
```

### 3. (W1 진행 중인 경우) worktree 4개 생성
```bash
git fetch origin
git worktree add ../inplay-w1-core    -b feat/w1-core    origin/feat/w1-core
git worktree add ../inplay-w1-coll    -b feat/w1-collector origin/feat/w1-collector
git worktree add ../inplay-w1-ingest  -b feat/w1-ingest  origin/feat/w1-ingest
git worktree add ../inplay-w1-ops     -b chore/w1-ops    origin/chore/w1-ops
```

(브랜치가 이미 main에 머지됐으면 worktree 생성 X)

### 4. Claude Code 띄우기
```bash
cd <worktree-dir>
claude
```

첫 메시지로 알려주기:
```
이 프로젝트는 inplay야. 
- HANDOFF.md → 현재 상태·다음 액션
- docs/PLAN.md → 전체 설계
- CLAUDE.md → 작업 룰

먼저 위 3개 읽고 시작해.
```

---

## 파일 구조

```
inplay/
├── HANDOFF.md          # 본 문서 (현재 상태 + 다음 액션)
├── README.md           # 빌드·실행 가이드
├── CLAUDE.md           # Claude Code 작업 룰
├── Makefile            # make compile / test / build / shell / mongo-up
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── .env.example        # 환경 변수 템플릿 (.env는 gitignored)
├── docs/
│   ├── PLAN.md         # 전체 설계 plan
│   └── adr/            # ADR-001~ (chore/w1-ops에서 시드)
├── modules/
│   ├── core/           # 공통 도메인
│   ├── collector/      # KBO·Statiz·Naver polling
│   ├── ingest/         # MongoDB 적재
│   ├── ml-inference/   # ONNX Runtime
│   ├── decision/       # WPA·위기 감지·알림 정책
│   ├── notify/         # Discord webhook
│   ├── journal/        # Notion API
│   └── api/            # Spring Boot main
├── python/trainer/     # PyTorch·LightGBM 학습 (운영 코드 X)
├── infra/
│   ├── compose/        # podman-compose.yml + mongo-init
│   └── k8s/            # 골격만 (W8)
└── scripts/
    └── podman-gradle.sh
```

---

## 주요 의사결정 요약

전체는 `docs/PLAN.md` §14 참조.

- **WebFlux 미채택** — Spring MVC + virtual thread
- **ONNX 경유** — Python 학습 + JVM 추론 분리
- **응원팀·라이벌리 데이터화** — 코드 하드코딩 X
- **데이터·모델 10구단 공유, 사용자 시점만 분리** (multi-tenant)
- **W4까지 채용 어필 최소 임계** (W5~W8은 buffer)
- **W7 RL bandit cut 가능** (사용자당 데이터 부족, v2 재시도)
- **빌드는 무조건 podman 컨테이너** (host JDK 무관)
- **`git push` 자동 X** — 사용자 직접 push
- **모든 ADR ≤ 300자**

---

## 합법 안전선 (불변)

`docs/PLAN.md` Context 섹션 참조.

1. robots.txt 준수, User-Agent 명시, 분당 1회 (경기 중 30초)
2. 매크로/자동매수 X — 정보·예측·알림·자동 일지까지만
3. 본인 사용용 (베타 5~10명), 대량 데이터 재배포 X
4. 새 외부 source 추가 시 ToS·robots 확인 + ADR 작성 게이트
5. 세션 쿠키·로그인 자동 사용 X — 공개 페이지만

---

## 외부 자원

| 자원 | URL/경로 | 비고 |
|---|---|---|
| GitHub repo | https://github.com/taeeho/InPlay | |
| Notion 루트 | https://www.notion.so/InPlay-35dbc507d1ef80abac81cb5318e96c78 | Claude Code MCP integration 연결됨 |
| Discord webhook | `.env`의 `INPLAY_DEFAULT_DISCORD_WEBHOOK` | gitignored |
| KBO 공식 | https://www.koreabaseball.com | 매너 폴링, robots allow `/Schedule/`만, **headless 렌더링 (ADR-009)** |
| Statiz | ~~https://www.statiz.co.kr~~ | **폐기** — robots.txt가 inplay UA 차단 + ToS 무단 이용 금지 |
| Naver Sports | ~~m.sports.naver.com~~ | **폐기** — robots `*` Disallow `/` |
| Daum Sports | ~~sports.daum.net~~ | **폐기** — SPA + 제3자 재배포 |

상세: `docs/adr/ADR-008-kbo-source-selection.md`, `docs/adr/notes/adr-008-source-survey.md`.

---

## 다음 즉각 액션 (이 문서 기준 시점, 2026-05-19)

1. **collector Playwright 통합 PR** (W1 마무리)
   - `ScheduleSource` 인터페이스 분리 (jsoup HTTP → headless 어댑터 가능하게)
   - Playwright Java 의존성 + Chromium 컨테이너 환경
   - ADR-009 운영 조건 코드화 (UA, route.abort, polling)
   - integration test에 podman socket 마운트 또는 별도 host 실행 환경
2. **W2 준비** — LightGBM Pre-game brief. historical 데이터는 사용자가 수동 또는 KBO 공식 협의 후 확보.
3. (선택) **KBO 공식 측 베타 사용 문의 메일** — 사용자 행동. 정식 협력 시 ADR-009 회색지대 해소.
