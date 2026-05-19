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
- [x] **core** — 도메인 record + 54 invariant tests (`0936d4c`)
- [x] **ops** — mongo-init + CI workflow + ADR-001~007 (`7fa2b34`)
- [x] **collector(HTTP)** — RestClient + RobotsGuard + RateLimiter + KboHttpScheduleSource + 25 tests (`f1b56aa`)
- [x] **ingest** — GameDocument + Repository + Mapper + IngestService + 9 unit / 4 integration tests (`461548f`)
- [x] **ADR-008** Accepted — KBO 공식 `/Schedule/Schedule.aspx` 채택. Statiz/Naver/Daum은 robots/ToS 차단으로 폐기 (`docs/adr/notes/adr-008-source-survey.md`).
- [x] **ADR-009** Accepted — Playwright headless 회색지대 운영 조건 (UA 명시, 분당 1회, `/ws/` AJAX `route.abort`, 베타 5~10명 한정).
- [x] **collector(headless)** — `ScheduleSource` 인터페이스 + `KboHeadlessScheduleSource` (Playwright) + `PageRenderer` 추상화 + `@ConditionalOnProperty` http/headless 분기 + 8 tests (`e14ff7d`)

### 완료 (W2 — 학습·추론 파이프라인 골격)
- [x] **trainer skeleton** — `python/trainer/win_prob/`. data_schema + feature engineering (7 features, 시간 누설 방지) + LightGBM train + ONNX export + Python parity check + fixture CSV (`2c7d149`)
- [x] **ml-inference Java ONNX** — `WinProbabilityFeatures` record + `WinProbabilityPredictor` (OrtSession 래퍼) + parity test (`@EnabledIf` ONNX 산출물 존재 시 자동 활성) + 6 unit tests (`72a4a59`)

### 진행 중 (W2 마무리)
- [ ] **Discord brief 발송** — `decision`에 brief 생성기, `notify`에 webhook client, `api`에 `@Scheduled` daily 08:00, application.yml 설정, 단위 테스트(mock RestClient)
- [ ] **(사용자 행동) trainer host venv 검증** — `cd python/trainer && python -m venv venv && source venv/bin/activate && pip install -r requirements.txt && pytest` 통과 확인
- [ ] **(사용자 행동) 학습 데이터 준비** — Wikipedia 시즌 페이지 + 본인 수기로 `season,date,home_team,away_team,home_score,away_score` CSV 모음
- [ ] **(사용자 행동) ONNX 학습 + commit** — host에서 train_lgbm.py + export_onnx.py 돌려 `modules/ml-inference/src/main/resources/models/v1/winprob.onnx` + `parity_sample.json` commit → Java parity test 자동 활성

### ⚠️ W1 결정적 발견 — 합법 자동 수집 source 부재
사용자 + Claude Code의 사람 확인(2026-05-19) 결과 한국 야구 일정 4개 후보(Statiz/Naver/Daum/KBO) 모두 ADR-005 보수 원칙(robots 자동 봇 직접 fetch)으로는 일정 데이터 수집 **불가**. 상세는 `docs/adr/notes/adr-008-source-survey.md`.

ADR-009로 headless 회색지대 운영 결정 — KBO `/Schedule/Schedule.aspx`만 Playwright로 SSR된 DOM 파싱, `/ws/`는 직접 호출 X. 베타 5~10명 한정, 데이터 재배포 X, KBO ToS에 자동 렌더링 금지 명시 발견 시 즉시 중단.

### 대기 (사용자 1회 작업)
- [ ] Notion DB 6개 수동 생성 (Notion UI에서 `/database` Inline. 칼럼 정의는 각 페이지 본문 참조). W6 전까지만 OK.
- [ ] (W7~W8 시점) 베타 친구 5~10명 명단 (이름·응원팀·Discord webhook URL)
- [ ] **KBO ToS 사람 확인** — `/Schedule/` 페이지 자동 렌더링 금지 조항 존재 여부. headless 모드 활성 직전 게이트 (ADR-009).
- [ ] **Playwright Chromium 셋업** — headless 모드 첫 실행 전. `Makefile`의 `GRADLE_IMAGE`를 `mcr.microsoft.com/playwright/java`로 swap이 가장 간단(A안). W1 데이터 게이트 통과 직전에.
- [ ] (선택) **KBO 공식 측 베타 사용 문의 메일** — 정식 협력 시 ADR-009 회색지대 해소.

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
- [x] LightGBM 승률 모델 코드 (Python) + ONNX export 파이프라인 — `python/trainer/win_prob/`
- [x] Java ONNX Runtime 통합 + parity test 스켈레톤 (`@EnabledIf` ONNX 산출물 존재 시 자동 활성)
- [ ] 매일 08:00 Discord brief 발송 — decision/notify/api 통합 (다음 작업)
- [ ] **데이터 게이트**: 사용자가 host venv에서 trainer 검증 + 학습 데이터 모으고 ONNX commit → parity 통과
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
| Wikipedia (ko) | https://ko.wikipedia.org/wiki/2025년_KBO_리그 | W2 학습 데이터 1차. CC-BY-SA. robots 깨끗 — 시즌별 페이지 시즌 요약·최종 순위·포스트시즌 결과만(144경기 row는 없음). |
| Statiz | ~~https://www.statiz.co.kr~~ | **폐기** — robots.txt가 inplay UA 차단 + ToS 무단 이용 금지 |
| Naver Sports | ~~m.sports.naver.com~~ | **폐기** — robots `*` Disallow `/` |
| Daum Sports | ~~sports.daum.net~~ | **폐기** — SPA + 제3자 재배포 |

상세: `docs/adr/ADR-008-kbo-source-selection.md`, `docs/adr/notes/adr-008-source-survey.md`.

---

## 다음 즉각 액션 (이 문서 기준 시점, 2026-05-19)

### 1. Discord brief 발송 (W2 마무리, Claude 작업)
- `modules/decision` — `WinProbabilityBrief` 생성기 (모델 추론 결과 + 홈/원정 + 사용자 응원팀 강조)
- `modules/notify` — `DiscordWebhookClient` (webhook POST, mock RestClient로 단위 테스트)
- `modules/api` — `@Scheduled` daily 08:00 trigger + application.yml 설정
- ONNX 모델 산출물 없어도 진행 가능 — `null` predictor 가드 또는 "모델 미준비" 메시지

### 2. (사용자 행동) trainer host 검증
```bash
cd python/trainer
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
pytest
```
- 12개 테스트(data_schema 5 + train_lgbm 1 + export_onnx 1 + features 추가)가 fixture로 통과해야.
- 통과 후 본격 데이터 모음 + 학습.

### 3. (사용자 행동) 학습 데이터 모음 + ONNX commit
- `python/trainer/win_prob/fixtures/` 대신 실 CSV (또는 별도 path) 준비.
- `python -m win_prob.train_lgbm --csv data/season.csv --out runs/v1`
- `python -m win_prob.export_onnx --booster runs/v1/winprob_lgbm.txt --onnx modules/ml-inference/src/main/resources/models/v1/winprob.onnx --sample modules/ml-inference/src/main/resources/models/v1/parity_sample.json`
- 두 산출물 commit → Java parity test 자동 활성.

### 4. (사용자 결정 후 Claude) KBO ToS 확인 + Playwright Chromium 셋업
- 사용자가 KBO `/Schedule/` 페이지의 ToS에 자동 렌더링/스크래핑 금지 조항 없는지 확인.
- 통과 시 Makefile `GRADLE_IMAGE`을 `mcr.microsoft.com/playwright/java`로 swap (A안) → integration test에서 실 KBO fetch 검증.
- W1 데이터 게이트 통과("7일 경기 100% 수집").

### 5. W3 시작 (네이버 라이브 polling)
- W2 brief가 7일 안정 발송 + W4 마일스톤 압박 시점에 진입.
- Naver Sports는 robots 차단 → DrissionPage Python sidecar 패턴 평가 (CLAUDE.md crawling-tool-selection wiki 참조).
