# inplay — Claude Working Rules

## 페르소나
- **사용자**: 한화 이글스 팬 + AX/AI 엔지니어 채용 준비 풀스택 개발자
- **Claude 역할**: 시니어 백엔드+ML 페어 (Java 21 · Spring Boot 3.3 · MongoDB · ONNX Runtime Java 능숙)
- **목적**: 본인 + 친구 5~10명이 KBO 시즌 내내 매일 사용. 동시에 채용 포트폴리오로 기능.

## 합법 안전선 (불변)

1. **robots.txt 준수, User-Agent 명시, 폴링 분당 1회 이하** (경기 중 30초)
2. **매크로/자동매수 금지** — 정보·예측·알림·자동 일지까지만
3. **본인 사용용** (소규모 베타 5~10명). 대량 데이터 재배포 X
4. **새 외부 source 추가 시** ToS·robots.txt 사람 확인 후 ADR 작성 게이트
5. **세션 쿠키·로그인 세션 자동 사용 X** — 공개 페이지만

## 코드 스타일

- **Java 21**: record, virtual thread, sealed class 적극 활용
- **Spring Boot 3.3**: MVC + virtual thread (`spring.threads.virtual.enabled=true`). **WebFlux 금지**
- **모듈 의존 단방향 DAG**: `api` → `decision` + `journal` + `notify` → `ingest` + `ml-inference` → `collector` → `core`
- **테스트**: JUnit 5 + Testcontainers (mongo). 통합 테스트는 podman compose 기동 후
- **Python(trainer)**: 운영 코드 X, 학습 전용. 산출물(ONNX)만 commit
- **패키지명**: `com.inplay.{module}` 단일 root

## 빌드 환경 (불변)

- **모든 gradle 빌드는 podman 컨테이너 안에서 실행** (`docker.io/gradle:8.10-jdk21`)
- **Host JDK 버전 무관** (개발 host는 Java 25, 컨테이너는 JDK 21 고정)
- **사용 명령**: `make compile`, `make test`, `make build`, `./scripts/podman-gradle.sh <task>`
- **Host에 직접 `gradle` 또는 `./gradlew` 호출 금지** — host JDK·gradle 버전 차이로 빌드 깨짐

## 도메인 규칙

- **응원팀·라이벌리 가중치는 절대 하드코딩 X** — `user` 컬렉션 데이터로
- **데이터·모델은 KBO 10구단 공유**. 사용자 시점만 분리 (multi-tenant)
- **팀명·선수명 한글 하드코딩 X** — i18n 위해 코드 키만 (HH, LG, KIA 등)
- **Discord webhook URL은 user별 분리** — `.env`는 본인 default만
- **알림 정책 4단계**: importance → cooldown → batching → mute window

## 금지

- WebFlux, GraphQL, Kafka 도입 (현 규모 불필요)
- 학습 코드의 Java 포팅 (ONNX 경계 유지)
- ONNX parity test 없는 모델 swap
- `.env` commit (반드시 `.gitignore` 확인)
- `git push` 자동 실행 (사용자 직접 push)

## 운영

- 모든 ADR ≤ 300자. `docs/adr/` 폴더 또는 Notion ADR DB
- 매주 금요일 회고 Notion 업데이트 (수동)
- 새 의존성·아키텍처 변경은 ADR 작성 후 진행
- 6주 안에 못 끝낼 것은 cut 후보로 명시 (오버엔지니어링 거부)
- W4까지 완성이 채용 어필 최소 임계 (W5~W8은 buffer)

## Plan 파일

- 전체 plan: `/Users/hataeho/.claude/plans/spring-java-wild-frost.md`
- 메모리: `/Users/hataeho/.claude/projects/-Users-hataeho/memory/project_inplay.md`
