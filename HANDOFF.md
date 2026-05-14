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

## 현재 상태 (마지막 갱신: 2026-05-14)

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

### 진행 중 (W1)
- [ ] **feat/w1-core** — modules/core/** 도메인 record + invariant 테스트 22+
- [ ] **chore/w1-ops** — infra/compose/mongo-init/ + .github/workflows/ci.yml + docs/adr/ADR-001~007
- [ ] **feat/w1-collector** (대기) — modules/collector/** KBO 일정 수집기·robots.txt 가드
- [ ] **feat/w1-ingest** (대기) — modules/ingest/** MongoDB Document·Repository·Testcontainers

머지 순서: `core` 먼저 → `collector`/`ingest`/`ops` 병렬 → main.

### 대기 (사용자 1회 작업)
- [ ] Notion DB 6개 수동 생성 (Notion UI에서 `/database` Inline. 칼럼 정의는 각 페이지 본문 참조). W6 전까지만 OK.
- [ ] (W7~W8 시점) 베타 친구 5~10명 명단 (이름·응원팀·Discord webhook URL)

---

## 마일스톤 체크리스트 (W1~W8)

`docs/PLAN.md` §8 / §9 참조. 게이트 통과 시 체크.

### W1 — 골격·수집 파이프라인
- [ ] core 도메인 record + invariant 테스트 22+
- [ ] collector KBO API + robots.txt 가드 + RestClient
- [ ] ingest MongoDB Document + Repository + Testcontainers
- [ ] infra mongo-init script + CI workflow + ADR 7개
- [ ] **Gate**: `make test` 통과, 7일 경기 100% 수집, robots 위반 0건

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
# /Users/hataeho/Documents/inplay            [main]
# /Users/hataeho/Documents/inplay-w1-core    [feat/w1-core]
# /Users/hataeho/Documents/inplay-w1-coll    [feat/w1-collector]
# /Users/hataeho/Documents/inplay-w1-ingest  [feat/w1-ingest]
# /Users/hataeho/Documents/inplay-w1-ops     [chore/w1-ops]
```

병렬 작업 룰:
- 각 브랜치는 자기 모듈 디렉토리만 수정
- `application.yml`은 ingest 한 명만 수정 (다른 yml은 한 명만)
- 루트 파일 (`build.gradle.kts`/`Makefile`/`README`/`CLAUDE.md`) 수정 시 별도 PR
- 커밋 후 `git push -u origin <branch>`. PR 머지는 사용자가 직접.

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
| KBO 공식 | https://www.koreabaseball.com | 매너 폴링, robots.txt 준수 |
| Statiz | https://statiz.sporki.com | 공개 통계 |

---

## 다음 즉각 액션 (이 문서 기준 시점)

1. (커밋·push 끝) 다른 worktree 작업자들이 main 변경 가져가게 알리기
2. core 작업 끝나면 main에 머지 → 다른 worktree에 rebase
3. ops 작업 끝나면 main에 머지
4. main 최신을 collector/ingest worktree에 rebase 후 작업 시작
5. W1 게이트 통과 (7일 경기 100% 수집) → W2 시작
