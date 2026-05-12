# ADR-003: MongoDB 단일 + timeseries (Kafka 미도입)

- Status: Accepted
- Date: 2026-05-12

## Context
분당 ≤ 600 이벤트, 베타 규모. Kafka/별도 TSDB는 운영 부담 대비 이득 없음.

## Decision
MongoDB 7 단일 인스턴스(rs0 single-node). `live_event`/`pitch_log`/`alert_event`는 timeseries 컬렉션. TTL 시즌+1개월.

## Consequences
change stream + `@Scheduled`로 충분. 트래픽 10배 증가 시 Kafka 재검토.
