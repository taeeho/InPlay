# ADR-009: collector headless 브라우저 — 회색지대 운영

- Status: Accepted
- Date: 2026-05-19

## Context
한국 야구 일정 페이지 다수가 SPA(JS-rendered). ADR-005 robots 준수를 유지하면서 화면에 노출된 데이터만 사용해야 한다. headless 브라우저는 사람 행위 시뮬레이션으로 robots 회색지대.

## Decision
Playwright(Java) 사용. 조건:
- 브라우저 UA: `Mozilla/5.0 ... inplay-headless/0.1 (+contact: ai@ccfm.co.kr)` — 정체 명시.
- 폴링: 분당 1회 / 경기 중 30초 (ADR-005 유지).
- robots disallow path는 직접 navigate 금지 + AJAX 호출도 `route.abort()`로 차단.
- 베타 5~10명 한정. 수집 데이터 재배포 X.
- 대상 사이트 ToS에 명시적 "자동 렌더링/스크래핑 금지" 발견 시 즉시 중단 + 사용자 통보.

## Consequences
- collector에 Playwright 의존 + Chromium 컨테이너 size ↑.
- 분당 호출 수 metric 노출 (Prometheus, W8).
- HANDOFF에 회색지대 위험 명시.
