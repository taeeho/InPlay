# inplay — KBO 통합 AI 동반시청 시스템

> **This is NOT a ticketing macro or auto-betting tool.** 본 프로젝트는 KBO 경기 시청 경험을 4단계로 자동화하는 합법 정보·예측·알림 시스템입니다.

## 한 줄 정의

**KBO 10구단 통합 AI 동반시청.** 사용자별 응원팀 설정 후 시청 전 brief / 시청 중 결정적 순간 push / 시청 후 자동 일지 / 비시즌 다른 종목 자동 전환.

## 핵심 가치

| 단계 | 무엇을 |
|---|---|
| **시청 전 (Pre-game)** | 매일 아침 Discord brief — 선발 분석·승률 예측·주목 포인트 |
| **시청 중 (In-game)** | 결정적 순간 실시간 push — "5회말 만루 위기, 류현진 87구, 지금 화면 켜라" |
| **시청 후 (Post-game)** | Notion에 시즌 일지 자동 작성 — highlights·시즌 누적·다음 경기 미리보기 |
| **비시즌 (11~3월)** | K리그·KBL·V리그 자동 활성화 + 한화 트레이드/FA 뉴스 자동 요약 |

## 기술 스택

- **Backend**: Java 21 LTS, Spring Boot 3.3 (MVC + virtual thread), Gradle Multi-Module 8개
- **DB**: MongoDB 7 (timeseries collection 활용)
- **ML/DL**: Python 학습 (PyTorch + LightGBM) → ONNX export → JVM 추론 (ONNX Runtime Java)
- **알림**: Discord webhook
- **인프라**: Podman compose (local-first), `.env` 기반 자동 fallback
- **정리**: Notion API (자동 일지)

## 모델 라인업 (3개 ensemble)

1. **승률 예측** (Pre-game) — LightGBM, holdout 50경기 accuracy ≥ 0.58 목표
2. **결정적 순간 감지** (In-game) — LightGBM classifier, F1 ≥ 0.7 목표
3. **투수 한계 예측** — LSTM, AUC ≥ 0.72 목표

## 합법 안전선 (불변)

- ✅ KBO 공식 API + Statiz (공개 통계) + 네이버 스포츠 라이브 (매너 폴링)
- ✅ robots.txt 준수, User-Agent 명시, 분당 1회 이하 (경기 중 30초)
- ❌ 매크로 / 자동 매수 / 자동 결제 / 자동 로그인
- ❌ 대량 데이터 재배포 / 비공개 페이지 접근

## 사용 모드

- **소규모 베타** (본인 + 친구 5~10명). 사용자별 응원팀·webhook·라이벌리 가중치 분리.
- **인증**: 간단 API key (Spring Security filter). OAuth/공개 SaaS는 v2.

## 시작하기 (개발자용)

```bash
# 1. 환경 변수 설정
cp .env.example .env
# .env 편집해서 본인 Discord webhook URL 등 입력

# 2. MongoDB + 의존 인프라 기동
podman compose -f infra/compose/podman-compose.yml up -d

# 3. Spring Boot 실행
./gradlew :modules:api:bootRun

# 4. 테스트
./gradlew test
```

## Plan 문서

전체 설계·마일스톤·검증 게이트는 `/Users/hataeho/.claude/plans/spring-java-wild-frost.md` 참조.

## 라이선스

본인 학습·포트폴리오 용도. 외부 배포 X.
