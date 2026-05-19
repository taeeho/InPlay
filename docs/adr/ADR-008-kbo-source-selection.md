# ADR-008: KBO 일정 source — 공식 사이트 + headless 렌더링

- Status: Accepted
- Date: 2026-05-19

## Context
W1 collector 일정 1차 source 결정. ADR-005 robots/UA/사람 ToS 확인 게이트.

## Decision
KBO 공식 `https://www.koreabaseball.com/Schedule/Schedule.aspx` 채택.
- `/Schedule/`는 robots allow. 그러나 페이지 데이터는 `/ws/Schedule.asmx/GetScheduleList` AJAX로 채워지고 `/ws/`는 robots disallow → **headless 브라우저로 SSR된 DOM만 파싱, `/ws/` 직접 호출 X**.
- 운영 조건(UA, polling, AJAX 차단 등)은 ADR-009.

Statiz · Naver Sports · Daum Sports는 robots/ToS 차단으로 폐기. 사람 확인 증거: [`notes/adr-008-source-survey.md`](notes/adr-008-source-survey.md).

## Consequences
- collector 재설계: jsoup-only → Playwright + jsoup 하이브리드. 별도 PR.
- `application.yml`에 `inplay.collector.kbo.schedule-url` 주입.
- 후속: 사용자가 KBO 공식 측에 베타 사용 문의(선택).
