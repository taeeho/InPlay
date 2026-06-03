# ADR-014: Notion 시즌 일지 — 자동 생성 + 중복방지 단일 인스턴스 전제

- Status: Accepted
- Date: 2026-06-03

## Context
W6 post-game 시즌 일지. 종료 경기를 사용자 시점으로 Notion `season_journal` DB에 자동 page 생성. brief/Discord와 동일한 graceful 패턴(외부 산출물·설정 없으면 skip).

## Decision

**journal 모듈** (`com.inplay.journal.season`):
- `SeasonJournalEntry` record + `JournalEntryGenerator` — `FINAL` + myTeam 출전 경기만 → 승/패/무 판정(홈/원정 스코어 기준). 그 외 `Optional.empty()`.
- `NotionClient` — `POST https://api.notion.com/v1/pages`, `Authorization: Bearer` + `Notion-Version` 헤더. 4xx/5xx/network 모두 swallow + 로그 + boolean (DiscordWebhookClient와 동일 정책).
- `NotionJournalPage` — Entry → request body. 칼럼(경기/날짜/시즌/결과/스코어)은 사용자가 Notion DB 수동 생성 시 맞춤. 스키마 불일치 → 4xx swallow.

**중복 방지** (`api SeasonJournalService`): `exists(season_journal) → Notion POST → save 마커` 순서.
- **단일 인스턴스 MVP 전제**: 동시 실행/다중 인스턴스면 Notion 중복 page 가능. Mongo unique index(`uniq_user_season_game`)는 저장만 막고 외부 Notion 생성은 못 막는다. 베타 규모(단일 인스턴스)에서 수용. 다중 인스턴스 필요 시 분산락 또는 pre-claim 패턴 후속.
- Notion 성공 후 `save` 실패는 `DuplicateKeyException` catch + 로그.

**경계**: Entry↔Document 매핑은 api에서(journal→ingest 빌드 의존은 PLAN DAG상 허용이나 현재 미사용). 인덱스는 mongo-init.js(프로젝트 컨벤션). multi-user loop는 W7, 뉴스 RSS 요약은 W8.

## Consequences
- 라벨/모델 없이 종료 경기만으로 일지 자동화 — 데이터 게이트 무관, 사용자는 Notion DB 6개 수동 생성 + `NOTION_API_KEY`/`NOTION_JOURNAL_DATABASE_ID`만 설정하면 동작.
- 단위 테스트 16개(journal 11 + api 5). `make test` BUILD SUCCESSFUL.
