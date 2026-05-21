# ADR-010: 미니 대시보드 — Thymeleaf + HTMX

- Status: Accepted
- Date: 2026-05-20

## Context
PLAN.md §0 "별도 웹 UI W8까지 cut" 결정 뒤집기. 채용 어필 측면에서 풀스택 한 줄 추가하고, 본인 사용 시 Discord 메시지만으로 부족한 "오늘 일정 한눈에" 욕구. 단, React+Vite 별도 빌드 파이프라인은 6주 일정에서 과함.

## Decision
- `modules/api`에 Thymeleaf (Spring Boot starter) + HTMX (CDN script tag) 단일 페이지 추가.
- 스타일: Pico.css CDN. JS 빌드 파이프라인·번들러·node_modules 없음.
- 첫 라우트: `GET /` — 오늘 KBO 일정 + brief preview + 최근 7일 결과. HTMX로 "오늘 일정" 부분 갱신.
- 인증: 기존 `spring-boot-starter-security` basic auth(dev/dev) 그대로. W6 API key filter 들어오면 그쪽으로 합류.

## Consequences
- api 모듈에 thymeleaf 의존 추가 (containerized api 이미지 size ~5MB ↑).
- W6 SportAdapter 추상화 시 view도 sport 무관 추상 필요 — `templates/_partial`로 분리 예정.
- Vaadin/Hilla 풀스택 Java 옵션은 cut. 셋업 무거움 + ML 도메인 색이 더 핵심.
