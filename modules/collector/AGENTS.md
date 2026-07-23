# collector 모듈 — 영역 규칙 (루트 AGENTS.md에 병합됨)

- 이 모듈은 합법 안전선의 최전선: robots.txt 준수, User-Agent 명시, 폴링 분당 1회 이하(경기 중 30초)를 코드 레벨에서 보장한다.
- 새 외부 source(사이트·API) 추가는 ToS·robots.txt 사람 확인 + ADR 작성 후에만.
- 로그인 세션·쿠키 사용 코드 추가 금지 — 공개 페이지만.
- 모듈 단독 테스트: 루트에서 `make shell` 후 `./gradlew :modules:collector:test` (또는 실 KBO fetch는 `make test-headless`).
