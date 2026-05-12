# ADR-001: Spring MVC + Virtual Thread (WebFlux 미채택)

- Status: Accepted
- Date: 2026-05-12

## Context
동시 경기 ≤ 5, 분당 ≤ 600 이벤트. 베타 5~10명.

## Decision
Spring Boot 3.3 MVC + Java 21 virtual thread (`spring.threads.virtual.enabled=true`). WebFlux 미채택.

## Consequences
blocking Mongo driver 그대로 사용. 동기 스타일로 디버깅/스택트레이스 단순. 처리량 부족 시 재검토.
