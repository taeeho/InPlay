"""Sequence feature engineering for LSTM input.

각 pitch row → 7 정규화 feature. (outing_id) 단위로 sliding window는 적용 X,
대신 전체 시퀀스를 [batch=1, seq_len, n_features] 로 padding 후 LSTM에 투입.

train 시: 한 outing의 마지막 pitch 시점에서 다음 PA의 label(allowed_baserunner) 예측.
inference 시: outing 진행 중 매 pitch마다 전체 시퀀스로 다시 호출 — sliding inference.
"""
from __future__ import annotations

import numpy as np
import pandas as pd

FEATURE_COLUMNS: tuple[str, ...] = (
    "pitch_seq_norm",          # pitch_seq / 100 (100구 기준)
    "pitch_speed_norm",        # (speed - 130) / 30 (130 kmh baseline, 30 spread)
    "pitch_type_norm",         # pitch_type_idx / 7
    "inning_norm",             # min(inning, 9) / 9
    "outs_norm",               # outs / 3
    "runners_on_norm",         # runners_on / 3
    "lineup_pos_norm",         # (batter_lineup_pos - 1) / 8
)

PAD_VALUE = 0.0


def build_sequences(events: pd.DataFrame, max_len: int = 120) -> tuple[np.ndarray, np.ndarray, list[str]]:
    """Returns (X: float32 [N, max_len, 7], y: int [N], outing_ids: list[str]).

    각 outing의 마지막 row의 label을 그 시퀀스 전체의 supervised label로 사용.
    seq_len > max_len 이면 뒷부분 max_len만 사용 (recent window).
    seq_len < max_len 이면 앞쪽을 PAD_VALUE로 패딩.
    """
    df = events.sort_values(["outing_id", "pitch_seq"]).reset_index(drop=True)

    outings: list[str] = []
    X_rows: list[np.ndarray] = []
    y_rows: list[int] = []

    for outing_id, group in df.groupby("outing_id", sort=False):
        feats = _normalize(group)
        if len(feats) > max_len:
            feats = feats[-max_len:]
        padded = np.full((max_len, len(FEATURE_COLUMNS)), PAD_VALUE, dtype=np.float32)
        padded[-len(feats):] = feats
        X_rows.append(padded)
        y_rows.append(int(group["allowed_baserunner"].iloc[-1]))
        outings.append(str(outing_id))

    if not X_rows:
        return (
            np.zeros((0, max_len, len(FEATURE_COLUMNS)), dtype=np.float32),
            np.zeros((0,), dtype=np.int64),
            [],
        )
    X = np.stack(X_rows, axis=0)
    y = np.asarray(y_rows, dtype=np.int64)
    return X, y, outings


def _normalize(group: pd.DataFrame) -> np.ndarray:
    rows = []
    for _, r in group.iterrows():
        rows.append([
            float(r["pitch_seq"]) / 100.0,
            (float(r["pitch_speed_kmh"]) - 130.0) / 30.0,
            float(r["pitch_type_idx"]) / 7.0,
            min(int(r["inning"]), 9) / 9.0,
            float(r["outs"]) / 3.0,
            float(r["runners_on"]) / 3.0,
            (int(r["batter_lineup_pos"]) - 1) / 8.0,
        ])
    return np.asarray(rows, dtype=np.float32)
