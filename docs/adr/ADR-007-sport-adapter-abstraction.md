# ADR-007: SportAdapter 추상화 (비시즌 종목 전환)

- Status: Accepted
- Date: 2026-05-12

## Context
KBO 비시즌(11~3월) 동안 시스템 idle 방지. 한화 트레이드/FA 뉴스는 별도 모듈로 연중.

## Decision
`SportAdapter` 인터페이스(`code`/`currentSeason`/`fetchSchedule`/`pollLive`/`brief`)에 KBO/K리그/KBL/V리그 어댑터 분리. `decision`/`notify`/`journal`은 어댑터에만 의존. W6는 K리그 stub만.

## Consequences
KBO adapter 비시즌엔 빈 schedule 반환 → orchestrator가 타 어댑터 활성화. 실데이터 연동은 시즌 종료 후.
