"""KBO in-game clutch(결정적 순간) classifier — LightGBM baseline.

목적: '이미 발생한' LiveEvent가 사후적으로 '결정적'이었는지 분류 (post-hoc classification).
      미래 이벤트 예측 X — feature는 we_home_after/before·outs·runners 등
      이벤트가 끝난 시점의 컨텍스트.

inference 입력 계약: features.FEATURE_COLUMNS 7개 (ClutchFeatures 와 동일 순서).
학습 입력 계약: data_schema.REQUIRED_COLUMNS 9개 (raw event row + supervised label).

라벨링 (clutch ∈ {0,1}): W3 데이터 게이트 통과 후 본인이 ride-along 5경기에서 매김.
fixture/sample_events.csv는 파이프라인 sanity check용 휴리스틱 라벨.

threshold 0.5는 기본값일 뿐 — calibration/체감 조정은 W4 라벨 수집 후 수행.
"""
