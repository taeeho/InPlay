# inplay — KBO 통합 AI 동반시청

> Claude Code · Codex CLI 공통 지침 (단일 진실). Claude 전용 설정은 CLAUDE.md / .claude/.

## 개요
- 목적: 본인 + 친구 5~10명이 KBO 시즌 내내 매일 사용. 동시에 AI 엔지니어 채용 포트폴리오.
- 스택: Java 21 · Spring Boot 3.3 · MongoDB · ONNX Runtime Java · Python(학습 전용)

## 빌드 · 테스트 (명령 검증: 2026-07-23 `make compile` BUILD SUCCESSFUL)
- **모든 gradle 빌드는 podman 컨테이너 안에서**: `make compile` / `make test` / `make build` (또는 `./scripts/podman-gradle.sh <task>`)
- **Host에서 직접 `gradle`·`./gradlew` 호출 금지** — host JDK 25 ≠ 컨테이너 JDK 21(`docker.io/gradle:8.10-jdk21`)
- MongoDB: `make mongo-up` / `make mongo-down`. 통합 테스트는 JUnit 5 + Testcontainers(mongo), podman compose 기동 후.
- 전체 타깃 목록: `make help`

## 합법 안전선 (불변)
1. robots.txt 준수, User-Agent 명시, 폴링 분당 1회 이하 (경기 중 30초)
2. 매크로/자동매수 금지 — 정보·예측·알림·자동 일지까지만
3. 본인 사용용 (소규모 베타 5~10명). 대량 데이터 재배포 X
4. 새 외부 source 추가 시 ToS·robots.txt 사람 확인 후 ADR 작성 게이트
5. 세션 쿠키·로그인 세션 자동 사용 X — 공개 페이지만

## 코드 스타일
- Java 21: record, virtual thread, sealed class 적극 활용
- Spring Boot 3.3: MVC + virtual thread (`spring.threads.virtual.enabled=true`). **WebFlux 금지**
- 모듈 의존 단방향 DAG: `api` → `decision` + `journal` + `notify` → `ingest` + `ml-inference` → `collector` → `core`
- Python(trainer): 운영 코드 X, 학습 전용. 산출물(ONNX)만 commit
- 패키지명: `com.inplay.{module}` 단일 root

## 도메인 규칙
- 응원팀·라이벌리 가중치 하드코딩 X — `user` 컬렉션 데이터로
- 데이터·모델은 KBO 10구단 공유, 사용자 시점만 분리 (multi-tenant)
- 팀명·선수명 한글 하드코딩 X — 코드 키만 (HH, LG, KIA 등)
- Discord webhook URL은 user별 분리 — `.env`는 본인 default만
- 알림 정책 4단계: importance → cooldown → batching → mute window

## 금지
- WebFlux, GraphQL, Kafka 도입 (현 규모 불필요)
- 학습 코드의 Java 포팅 (ONNX 경계 유지)
- ONNX parity test 없는 모델 swap
- `.env` commit
- `git push` 자동 실행 (push는 사용자가 직접 — pre-push hook으로도 차단됨)

## 운영
- 모든 ADR ≤ 300자. `docs/adr/` 또는 Notion ADR DB. 새 의존성·아키텍처 변경은 ADR 후 진행.
- 6주 안에 못 끝낼 것은 cut 후보로 명시 (오버엔지니어링 거부). W4 완성이 채용 어필 최소 임계.
