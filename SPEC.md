# inplay — 기술 스펙 (무엇을, 어디에, 무엇으로)

> "어떤 기술을 어디에 쓰는가" 한눈에. 전체 설계는 `docs/PLAN.md`, 현재 상황은 `STATE.md`.

## 핵심 스택

| 레이어 | 기술 | 비고 |
|---|---|---|
| 백엔드 | Java 21 + Spring Boot 3.3 (MVC + virtual thread) | **WebFlux/GraphQL/Kafka 금지** |
| DB | MongoDB 7 (timeseries 포함, replica set rs0) | |
| ML 추론 | ONNX Runtime Java | JVM 경계 |
| ML 학습 | Python (PyTorch + LightGBM) → ONNX export | 운영코드 X, 산출물(ONNX)만 commit |
| 빌드 | Gradle 8.10, **podman 컨테이너 JDK21 고정** | host `gradle`/`gradlew` 직접 호출 금지 |
| 컨테이너 | Podman (mongo:7 필수, ollama 선택) | api 컨테이너화는 W8 |
| 패키지 | `com.inplay.{module}` | |

## 데이터 수집 — 어디서 무엇으로

| 정보 | source | 도구 | 합법성 |
|---|---|---|---|
| **KBO 일정·결과** | `koreabaseball.com/Schedule/Schedule.aspx` | **Playwright headless** (`KboHeadlessScheduleSource`) — 일정이 `/ws/` AJAX로만 렌더돼 SSR된 DOM 파싱 | robots allow `/Schedule/`, ToS 자동수집 금지 없음 (ADR-008/009) |
| KBO raw HTTP | 동일 | `KboHttpScheduleSource` (RestClient + jsoup) — 빈 골격만 받음, fixture/폴백용 | |
| 라이브 이벤트 | (W3 운영 미정) | headless polling 30초 | ADR-009 |
| 학습 데이터 | Wikipedia 시즌 페이지 + 수기 CSV | requests / 수기 | CC-BY-SA |
| ~~Statiz/Naver/Daum~~ | - | **전부 폐기** | robots `*` Disallow (ADR-008) |

- **파싱**: `KboScheduleParser` (jsoup). 한글 팀명 → 코드 키 번역은 이 경계 계층에서만 (anti-corruption).
- **가드**: `RobotsGuard`(robots.txt 캐시 24h) + `PollingRateLimiter`(분당 1회/경기중 30초) + UA 명시.
- **개발 보조**: 실 KBO DOM 구조 확인은 **Playwright MCP**(운영 아님). 봇탐지 강한 사이트는 DrissionPage.

## 출력·알림 — 어디에 무엇으로

| 대상 | 용도 | 도구 |
|---|---|---|
| Discord webhook | pre-game brief, 실시간 clutch push | RestClient (`DiscordWebhookClient`), **사용자별 URL 분리** |
| Notion API | 시즌 자동 일지 | RestClient `POST /v1/pages` (`NotionClient`), DB 수동 생성 |
| 웹 대시보드 | 오늘 일정 한눈 보기 (`GET /`) | **Thymeleaf + HTMX + Pico.css (CDN)**, 번들러 없음 (ADR-010) |

## 모델 4종 (ensemble)

| 모델 | 알고리즘 | 서빙 |
|---|---|---|
| 승률 예측 | LightGBM (→ LSTM W5 cut가능) | Python→ONNX→Java |
| 결정적 순간(clutch) | LightGBM classifier | Python→ONNX→Java |
| 투수 한계 | LSTM | Python→ONNX→Java |
| 알림 정책 | LinUCB bandit (W7, cut가능) | Java native |

- **parity test 강제**: Python 추론 vs Java ONNX 1e-4 이내. ONNX 산출물 없으면 `@EnabledIf`로 skip.
- WPA는 모델 아님 — rule-based (RE24 + 정규근사 WE, `decision/wpa`).

## 인증·멀티테넌트

- **API key** (Spring Security filter), `api_key_hash = sha256`. OAuth는 v2.
- 데이터·모델은 KBO 10구단 **공유**, 사용자 시점만 분리(`user`/`brief`/`alert`/`journal`).
- 응원팀·라이벌리 가중치 **하드코딩 X** — `user` 컬렉션 데이터.

## 검증 도구

- JUnit 5 + AssertJ + Testcontainers(mongo, `integrationTest` sourceSet).
- 명령: `make test`(전체), `make test-headless`(실 KBO fetch, `KBO_LIVE=1` + Playwright 이미지).
