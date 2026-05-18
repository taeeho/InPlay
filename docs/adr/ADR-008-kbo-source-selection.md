# ADR-008: KBO 일정·결과 source 선택

- Status: **Proposed (사용자 확정 대기)**
- Date: 2026-05-18

## Context
W1 collector가 호출할 KBO 일정 1차 source를 결정해야 한다. ADR-005에 따라 robots.txt 준수 + UA 명시 + 사람의 ToS 확인 + ADR 게이트.

## Options
| # | Source | robots | 렌더링 | jsoup OK | W1 collector 재설계 |
|---|---|---|---|---|---|
| A | KBO `/Schedule/Schedule.aspx` | allow (`/ws/`는 disallow) | JS 렌더링 | 불가 | headless 도입 필요 |
| B | Statiz (`statiz.sporki.com`) | 미확인 (사람 확인) | 정적 추정 | 가능 | 그대로 사용 |
| C | Naver Sports 일정 | 미확인 (사람 확인) | JS 추정 | 불가 | W3 라이브와 통합 |

## Decision (draft)
*사용자 확정 필요.* 추천: **B(Statiz)**.
- KBO `/ws/` disallow 신호로 비공식 AJAX endpoint는 회피.
- KBO HTML 페이지(JS 렌더링)는 현재 collector 스택(jsoup) 범위를 벗어남.
- Statiz는 봇탐지 약한 정적 페이지로 알려져 있어 W1 collector 그대로 사용 가능. KBO 1차 source는 아니므로 README에 명시.
- C(Naver)는 W3 라이브와 source 통합 이점이 있지만, W1 일정 수집에는 과한 의존.

## Consequences
- 확정 시 `application.yml`에 `inplay.collector.kbo.schedule-url` 주입.
- 확정 후 ADR `Status: Accepted`로 갱신, source-specific HTML fixture로 `KboScheduleParser` 검증 보강.
- 사람의 ToS 확인 기록은 본 ADR 하단에 추가.

## Pending action (사용자)
- [ ] 옵션 A/B/C 선택
- [ ] 선택한 source의 robots.txt + 이용약관 사람 확인 (ADR-005 게이트)
- [ ] 확정 URL 통보
