# ADR-004: Multi-tenant — 데이터·모델 공유 + 사용자 시점만 분리

- Status: Accepted
- Date: 2026-05-12

## Context
KBO 10팀 데이터·모델은 모든 사용자 동일. 응원팀·라이벌리·webhook만 사용자별.

## Decision
공유: `team`/`player`/`game`/`live_event`/`pitch_log`/`pitcher_stat_daily`/`model_snapshot`. 사용자별: `user`/`pre_game_brief`/`alert_event`/`season_journal`. 가중치 하드코딩 금지 — `user` 컬렉션으로.

## Consequences
사용자 추가 비용 ≈ 0. v2에서 OAuth/공개 SaaS 확장 시 그대로 재사용.
