# ADR-013: 투수 한계 LSTM — 사전 스켈레톤 + leak/drift 가드 계획

- Status: Accepted
- Date: 2026-05-21

## Context
W5 투수 한계 예측은 LSTM (PyTorch → ONNX → Java). 라벨링은 W3 pitch_log 수집 이후. 본작업 전에 파이프라인 스캐폴딩을 미리 깔아두지 않으면 데이터 게이트 통과 후 모듈 골격 짜는 데 시간 낭비. 사전 작업으로 "라벨만 채우면 학습→export→Java 추론"까지 자동 이어지게 한다.

## Decision

**Python trainer (`python/trainer/pitcher_limit/`)**:
- LSTM hidden=16 single layer + sigmoid head. 베타 데이터셋(~수백 outing) 학습 가능.
- Input shape `[B, max_seq_len=120, 7]`. seq_len > max → 끝 max만 사용 (recent window). seq_len < max → 앞쪽 0.0 padding.
- 7 정규화 feature: `pitch_seq/100`, `(speed-130)/30`, `type/7`, `inning/9`, `outs/3`, `runners/3`, `(lineup-1)/8`.
- 라벨 정의 (post-hoc supervised): `allowed_baserunner ∈ {0,1}` — pitch 이후 PA 종료 시 hit/walk/HBP 발생 여부. W3 pitch_log timeseries에서 자동 추출 가능.
- ONNX export: `torch.onnx.export` opset 14 + `dynamic_axes={'input':{0:'batch'}}` → 1개 추론 + batched 추론 둘 다 지원. Python↔ONNX parity 1e-4.

**Java (`modules/ml-inference/pitcher/`)**:
- `PitchSnapshot` (7-field record, [0,1] invariant) + `PitcherLimitFeatures` (List → `[1, maxSeqLen, 7]` tensor, Python `build_sequences`와 동일 trim/pad).
- `PitcherLimitPredictor` (OrtSession, sigmoid 추출). `@EnabledIf` parity test — ONNX 산출물 commit 시 자동 활성.

**Split semantics** (Codex 검토 반영):
- 현재 outing 단위 split → "outing leak" 방지 (같은 등판의 pitch가 train/test 동시 출현 차단).
- 진짜 "pitcher leak"(같은 투수의 다른 등판이 분산)은 차단 X. W5 본작업에서 `sklearn GroupKFold` + `pitcher_id` 그룹으로 확장.

**Drift 가드**: 정규화 상수가 Python/Java 양쪽에 하드코딩. W5 본작업에서 `model_metadata.json` (max_seq_len, feature scaling, pitch_type 매핑) export → Java가 metadata 읽고 검증.

## Consequences
- W5 본작업 진입 시 ONNX commit만으로 Java 추론 활성 (zero-wiring cost).
- pitcher leak / metadata drift는 본작업 게이트로 남김 — 사전 작업의 의도된 한계.
- ONNX dynamic batch axis로 향후 실시간 multi-pitcher 추론 가능.
