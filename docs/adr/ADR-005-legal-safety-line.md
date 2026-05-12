# ADR-005: 합법 안전선 (불변)

- Status: Accepted
- Date: 2026-05-12

## Context
이전 ticketer/park-pulse(티켓 매크로) 위법 리스크로 폐기. inplay는 정보·예측·알림까지만.

## Decision
robots.txt 준수, UA 명시, 폴링 분당 1회(경기 중 30초). 매크로/자동결제/세션 쿠키 자동 사용 금지. 새 외부 source 추가 시 ToS·robots 사람 확인 + ADR 게이트.

## Consequences
`collector` 시작 시 robots fetch·24h 캐시, 위반 detect 즉시 stop + Discord 경고. 본인 + 친구 5~10명 베타에 한정.
