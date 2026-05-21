"""KBO pitcher-limit (투수 한계) prediction — LSTM baseline.

목적: '이번 plate appearance에서 안타/볼넷/HR을 허용할 확률' (post-hoc 라벨링 가능한 outcome).
      sequence input: 한 투수의 한 outing에서의 pitch-level 컨텍스트 시퀀스.

학습 입력 계약: data_schema.REQUIRED_COLUMNS (pitch-level CSV, pitcher_id + outing_id로 그룹).
inference 입력 계약: features.FEATURE_COLUMNS 7개 × seq_len (PitcherLimitFeatures).

라벨링 (allowed_baserunner ∈ {0,1}): W5 데이터 수집은 W3 라이브 polling 후 pitch_log
컬렉션에서 자동 생성 가능. fixture/sample_pitches.csv는 sanity check용 휴리스틱.

W4 clutch와 달리 LSTM이라 PyTorch로 학습 후 torch.onnx.export. Python ↔ ONNX runtime
parity 1e-4 검증 동일.
"""
