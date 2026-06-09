# inplay — 전체 워크플로우

> 프로젝트가 어떻게 돌아가는가(런타임) + 어떻게 개발하는가(개발). 설계 근거는 `docs/PLAN.md`.

## 1. 사용자 경험 흐름 (4단계 자동화)

```
시청 전        시청 중           시청 후         비시즌
  │             │                │              │
브리핑      실시간 push        자동 일지     종목 전환
(08:00)    (clutch 감지)      (23:30)      (SportAdapter)
Discord     Discord           Notion        K리그/KBL/V리그
```

## 2. 런타임 데이터 흐름

```
[KBO /Schedule/  ── Playwright headless]
            │
        collector  (robots gate + rate limit + UA + jsoup 파싱)
            │
        ingest     (debounce 60s + dedupe + timeseries 라우팅)
            │
   ┌────────┼────────────┐
 game    live_event(TS)  pitch_log(TS)
   │        │             │
   └───┬────┴──────┬──────┘
       │           │
  decision:WPA   ml-inference:ONNX (승률/clutch/투수한계)
       └─────┬─────┘
        notify policy (importance→cooldown→batching→mute)
       ┌─────┼──────────┐
  Discord push   alert_event   season_journal → Notion API
```

## 3. 모듈 의존 DAG (단방향)

```
api → decision + journal + notify → ingest + ml-inference → collector → core
```

각 모듈은 위 방향으로만 의존. `decision/notify/journal`은 `SportAdapter` 추상화에만 의존(종목 무관).

## 4. ML 워크플로우 (Python 학습 → JVM 추론)

```
python/trainer/{win_prob,clutch,pitcher_limit}/
  data_schema → features → train(LightGBM/LSTM) → export_onnx → parity check
            │
   modules/ml-inference/src/main/resources/models/v{n}/*.onnx  (commit)
            │
   Java {WinProbability,Clutch,PitcherLimit}Predictor (OrtSession 래퍼)
            │
   parity test (@EnabledIf, ONNX 존재 시 자동 활성, 1e-4 이내 일치)
```

- 학습 코드는 **Java 포팅 금지** (ONNX 경계 유지).
- 모델 swap은 parity test 통과 필수.

## 5. 개발 워크플로우

- **브랜치**: `main`에서 직접 작업 (2026-06-09~, 브랜치/worktree 안 팜).
- **빌드**: `make compile` / `make test` / `make build` — 전부 podman 컨테이너. **host `gradle`/`gradlew` 직접 호출 금지**.
- **실 KBO 검증**: `make test-headless` (`KBO_LIVE=1` + Playwright Chromium 이미지).
- **커밋**: 메시지 사전 텍스트로 제시 → 사용자 승인 → 커밋. **`git push`는 사용자가 직접**.
- **새 외부 source**: ToS·robots 사람 확인 → ADR 작성 → 구현 (게이트).
- **ADR**: ≤ 300자, `docs/adr/`. 아키텍처/의존 변경은 ADR 먼저.

## 6. 합법 안전선 (불변, 모든 흐름에 적용)

1. robots.txt 준수, UA 명시, 폴링 분당 1회(경기중 30초)
2. 매크로/자동매수 X — 정보·예측·알림·자동 일지까지만
3. 본인+베타 5~10명 사용용, **raw 데이터 재배포·복제·상업 이용 X** (KBO ToS)
4. 세션 쿠키·로그인 자동 사용 X — 공개 페이지만
5. headless: 페이지 렌더 종속 XHR(`/ws/`)은 허용, 크롤러 직접 호출은 금지 (ADR-009)

## 7. 목표 운영 주기 (시즌 중)

```
08:00         경기 시작~종료        23:30        비시즌
brief 발송 →  30초 polling+push  →  일지 생성  →  종목 전환
```
