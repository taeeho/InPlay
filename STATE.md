# inplay — 현재 상황 (2026-06-09)

> 지금 어디까지 됐나. 앞으로 할 일은 `HANDOFF.md`, 전체 흐름은 `WORKFLOW.md`, 기술은 `SPEC.md`.

## 한 줄

코드 골격은 W6까지 완료. **실데이터 파이프라인 입구(KBO 일정 수집)를 방금 뚫음** — 실 KBO 125경기 100% 파싱 검증. 다음은 실 polling 운영 wiring + ONNX 모델 학습.

## 모듈별 상태

| 모듈 | 상태 |
|---|---|
| core | ✅ 도메인 record + invariant. **(2026-06-09) `KboTeam` 두산 누락 버그 수정 (HAN→DOOSAN)** |
| collector | ✅ HTTP+headless+robots+ratelimit. **`KboScheduleParser` 실 DOM 전면 재작성 + 실데이터 125경기 100% 검증** |
| ingest | ✅ game / live_event(TS) / season_journal / user Document + repository + mapper |
| ml-inference | ✅ ONNX 래퍼 스켈레톤 3종 (winprob/clutch/pitcher) + parity test (**모델 산출물 없어 skip 상태**) |
| decision | ✅ WPA 엔진(RE24+WE) + clutch 감지 + 4단계 알림 정책 |
| notify | ✅ Discord client + brief/clutch formatter |
| journal | ✅ Notion client + 시즌 일지 생성 |
| api | ✅ scheduler(brief/journal) + config + Thymeleaf 대시보드 + API key 인증 |

## 테스트

~190+ unit. core + collector 검증 통과(2026-06-09). 전체 `make test` 회귀는 본 작업 마지막에 1회 실행.

## 막힌 것 (대부분 사용자 행동 / 운영 게이트)

1. **ONNX 모델 미학습** — 실 CSV 학습 → `winprob/clutch/pitcher.onnx` commit 필요. 현재 모든 predictor가 graceful skip. brief/push는 fallback만.
2. **실 polling 미운영** — collector headless ↔ ingest ↔ 스케줄러 wiring 후 mongo 적재 필요 (코드 일부, 운영 미가동).
3. **Notion 일지 미동작** — `season_journal` DB 생성 + `NOTION_API_KEY`/`DB_ID` 설정 필요(사용자 1회).
4. **`make test-headless` 미검증** — 코드만 준비. Playwright 이미지 태그(`v1.49.0-noble`) 확인 + 컨테이너 실행 안 함.

## 주차 진척

| 주 | 코드 | 데이터/운영 게이트 |
|---|---|---|
| W1 골격·수집 | ✅ | ✅ KBO 파싱 검증 (실 적재 운영은 polling wiring 후) |
| W2 brief+승률ML | ✅ | ✗ ONNX 학습 |
| W3 실시간+WPA | ✅ | ✅ 라이브 source 해소(ADR-009 개정) / 운영 ✗ |
| W4 clutch+push | ✅ 🎯채용 최소 임계 | ✗ ride-along 5경기 |
| W5 투수 LSTM | ✅ 스켈레톤 | ✗ 학습 |
| W6 일지+User | ✅ (journal/user/apikey) | ✗ SportAdapter K리그 stub / Notion DB |
| W7~W8 | 미착수 | - |

## 최근 변경 (2026-06-09, push 완료)

- KBO ToS 사람 확인 (자동수집 금지 없음 / 복제·재배포·상업 금지 제약 기록)
- ADR-009 개정: `/ws/` XHR abort 제거 (일정이 `/ws/` AJAX로만 렌더)
- `KboTeam` DOOSAN 수정, `KboScheduleParser` 실 DOM 재작성, 관련 테스트 재작성
- `KboHeadlessLiveTest` + Makefile `test-headless` 타겟
- 문서 4종 정리 (SPEC/STATE/WORKFLOW 신설 + HANDOFF 재정리)

## 코드 점검 (2026-06-09)

병렬 진단(미사용/중복/죽은설정) + 오탐 검증 수행. 결과:
- **죽은 코드 2개 제거**: `WinProbabilityBrief.probability()`(Optional 래퍼, 미사용), `NotionProperties.pageId`(+yaml `page-id`, 미사용)
- **보존**(오탐·예정): `DashboardRow.isFinal()`(Thymeleaf 5곳 사용), `AlertCategory` 4상수(예정 알림 어휘), `inplay.collector.polling/user-agent/rate-limit` yaml(polling wiring 시 외부화 예정)
- **미적용 중복**(성급한 추상화 경계 — 필요 시 정리): `"Asia/Seoul"` 4곳, HTTP 타임아웃(3/5s) 3곳, 시즌시작일 `Month.MARCH,1` 2곳, 외부 client 에러처리 패턴(Discord/Notion)
