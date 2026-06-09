# inplay — Handoff (앞으로 할 일)

> **이 문서는 "다음에 뭘 할지"만.**
> 현재 상황 → `STATE.md` · 전체 흐름 → `WORKFLOW.md` · 기술 사용처 → `SPEC.md` · 전체 설계 → `docs/PLAN.md` · 작업 룰 → `CLAUDE.md`.

---

## 즉시 다음 액션 (우선순위 순)

### 1순위 — 실 polling 운영 wiring  (W1/W3 데이터 게이트 운영화)
실데이터 입구(KBO 파싱)는 검증 끝. 이제 실제로 mongo에 적재되게 연결.
- [ ] `make test-headless` 로 실 KBO fetch 1회 검증 — 먼저 Playwright 이미지 태그(`mcr.microsoft.com/playwright/java:v1.49.0-noble`) 존재 확인, 안 맞으면 태그 교정
- [ ] `KboHeadlessScheduleSource` → `ingest`(GameDocument upsert) → 스케줄러(`@Scheduled`) wiring
- [ ] `make mongo-up` 후 7일치 fetch → `game` 컬렉션 적재 확인 (mongo-express)
- [ ] W3 라이브: `live_event` 30초 polling 운영 연결

### 2순위 — ONNX 모델 학습  (W2/W4/W5 데이터 게이트)
현재 모든 predictor가 산출물 없어 skip. 실확률·clutch·투수한계 활성화.
- [ ] (사용자) `cd python/trainer && python -m venv venv && pip install -r requirements.txt && pytest`
- [ ] (사용자) 학습 데이터 CSV 모음 (Wikipedia 시즌 + 수기)
- [ ] win_prob 학습 → `modules/ml-inference/.../models/v1/winprob.onnx` + parity_sample commit → parity 자동 활성 → brief 실확률
- [ ] clutch ride-along 5경기 라벨링 → `clutch.onnx` (precision ≥ 0.7)
- [ ] pitcher `pitch_log` 수집 → `pitcher.onnx` (GroupKFold pitcher_id + model_metadata.json)

### 3순위 — Notion 일지 실동작  (W6 데이터 게이트)
- [ ] (사용자) Notion `season_journal` DB 생성 — 칼럼: 경기(title)/날짜(date)/시즌(number)/결과(select 승·패·무)/스코어(rich_text)
- [ ] (사용자) `NOTION_API_KEY` + `NOTION_JOURNAL_DATABASE_ID` 설정 → 자동 동작

---

## W6 잔여 코드
- [ ] `SportAdapter` 추상화 + K리그 stub

## W7 — 사용자별 설정 + 알림 정책 분리
- [ ] 사용자별 my_team/rivalry_weights/mute_window/webhook 설정 API
- [ ] 알림 정책 엔진 사용자별 분기
- [ ] (cut 가능) LinUCB bandit
- [ ] Gate: 본인 + 가짜 LG팬·KIA팬 시뮬 → 각자 시점 brief/push 정상

## W8 — 베타 onboarding + 데모
- [ ] 친구 5명 onboarding (user 등록 + API key/webhook)
- [ ] 한화 트레이드/FA 뉴스 RSS + LLM 요약
- [ ] 데모 영상 3분 + 아키텍처 PDF + README 정리
- [ ] Spring Boot api 컨테이너화 (부팅 < 30s)

---

## 사용자 1회 작업 체크리스트
- [x] GitHub repo · Discord webhook · Notion 루트 페이지 · 라이벌리 가중치
- [x] KBO ToS 사람 확인 (2026-06-09 완료 — 자동수집 금지 없음, 복제·재배포·상업 금지 제약)
- [ ] Notion DB 6개 수동 생성 (W7 전까지)
- [ ] `season_journal` DB + `NOTION_API_KEY` 설정
- [ ] 베타 친구 5~10명 명단 (이름·응원팀·webhook) — W7~W8
- [ ] (선택) KBO 공식 측 베타 사용 문의 메일 — ADR-009 회색지대 정식 해소

---

## 해소된 블로커 (참고)
- ~~합법 자동 수집 source 부재~~ → ADR-009 개정(2026-06-09): KBO `/Schedule/` headless + `/ws/` XHR 허용. 실 125경기 100% 파싱 검증.
- ~~KBO ToS 자동 렌더링 금지 조항 우려~~ → 사람 확인 결과 해당 조항 없음.
- ~~`KboTeam` 두산 누락~~ → DOOSAN 수정.
