# ADR-009: collector headless 브라우저 — 회색지대 운영

- Status: Accepted
- Date: 2026-05-19

## Context
한국 야구 일정 페이지 다수가 SPA(JS-rendered). ADR-005 robots 준수를 유지하면서 화면에 노출된 데이터만 사용해야 한다. headless 브라우저는 사람 행위 시뮬레이션으로 robots 회색지대.

## Decision
Playwright(Java) 사용. 조건:
- 브라우저 UA: `Mozilla/5.0 ... inplay-headless/0.1 (+contact: ai@ccfm.co.kr)` — 정체 명시.
- 폴링: 분당 1회 / 경기 중 30초 (ADR-005 유지).
- robots disallow path(`/ws/` 등)는 크롤러가 **직접·독립적으로 navigate/fetch 금지**. 단 페이지 렌더링이 자동 트리거하는 `/ws/` XHR은 사람 브라우저와 동일하게 허용(`route.abort()` 안 함) — robots는 크롤러의 직접 path 탐색을 규율하지 브라우저 종속 리소스 로딩까지 막지 않는다는 해석.
- 베타 5~10명 한정. 수집 데이터 재배포 X.
- 대상 사이트 ToS에 명시적 "자동 렌더링/스크래핑 금지" 발견 시 즉시 중단 + 사용자 통보.

### /ws/ XHR 정책 개정 (2026-06-09)
실검증 결과 KBO 일정 데이터는 SSR된 DOM에 없고 `/ws/Schedule.asmx/GetScheduleList` AJAX로만 렌더된다. 기존 결정(`/ws/` `route.abort()`)으로는 테이블이 비어 수집 불가. → abort 패턴 제거. 페이지 렌더가 자동 발생시키는 `/ws/` XHR은 허용, 크롤러가 `/ws/`를 직접 호출하는 것은 계속 금지. Playwright MCP로 실 페이지 125경기 100% 파싱 확인.

### KBO ToS 확인 결과 (2026-06-09) — 운영 불변 제약 추가
KBO 이용약관 사람 확인(`docs/adr/notes/adr-008-source-survey.md`): 자동 렌더링/크롤링 명시 금지는 없음(→ headless 진행 OK). 단 제16·17조에 복제·재배포·상업적 이용 금지 조항 존재. 이에 따라:
1. MongoDB 적재는 본인 시청 보조 캐시 수준 — raw 영구 아카이브·공개 X.
2. 베타 친구 brief/push는 raw 일정표 재배포 X, ML 예측·WPA 등 자체 가공물 + 공개 정보 요약만.
3. raw 데이터 판매·출판·방송·공개 배포·영리 이용 절대 금지.

## Consequences
- collector에 Playwright 의존 + Chromium 컨테이너 size ↑.
- 분당 호출 수 metric 노출 (Prometheus, W8).
- HANDOFF에 회색지대 위험 명시.
