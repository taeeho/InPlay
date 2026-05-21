# ADR-011: WPA 엔진 — rule-based MVP + KBO 캘리브레이션 plan

- Status: Accepted
- Date: 2026-05-21

## Context
W3 결정적 순간 감지의 입력으로 WPA(Win Probability Added)가 필요. KBO RE24/WE 룩업 테이블·실제 게임 시퀀스가 모이는 건 W3 데이터 게이트 이후. 그 전까지 모든 후속 작업이 막힘.

## Decision
**Phase 1 (W3 코드 게이트)** — rule-based MVP:
- RE24: MLB FanGraphs 2010-2020 baseline (`RunExpectancy.RE24_MLB_BASELINE`).
- WE: 홈팀 시점 정규분포 근사 (`mean·stddev_per_half_MLB`, Abramowitz erf).
- `wpa_after` = 절대 `WE_home` (델타 X). ΔWE는 prev/curr 차이로 다운스트림 복원.
- 부호·순서 invariant 27 테스트로 강제, 절대값은 KBO 캘리브레이션 전까지 보수적.

**Phase 2 (W3 데이터 게이트 후)**:
- `python/trainer/win_prob/run_expectancy.py` 1시즌 pitch_log로 KBO RE24 재계산.
- WE 정규분포 σ·μ를 KBO 분포로 fit.
- model swap 시 `live_event.model_version` 필드 추가 (이전 wpa_after 값과 출처 분리).

**알려진 한계** (MVP 인정):
- `gameOver=false` 하드코딩 → 게임 종단 WE가 {0,1}로 수렴 안 함. W4 `finalize(endEvent, GameOutcome)` pass로 처리.
- Caffeine cache dedupe는 단일 프로세스 휘발성. 재시작·다중 인스턴스에서 중복 가능 → W4/W5 전 영속 idempotency ledger 검토.

## Consequences
- 채용 어필 시 "rule-based baseline → 실데이터 캘리브레이션" 2단 스토리.
- W3 데이터 게이트 통과 전까지 모든 WPA 절대값은 "보수적 근사"로 해석.
- 모델 버전 추적 필드 추가는 KBO 캘리브레이션 cutover 전 필수 게이트.
