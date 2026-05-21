"""Pitch-level CSV 입력 계약. 한 row = 한 pitch + 그 plate appearance의 결과(label).

label = 1 if 다음 PA에서 baserunner 허용 (hit/walk/HBP), 0 otherwise.
W3 pitch_log timeseries에서 자동 추출 가능.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Final

import pandas as pd

REQUIRED_COLUMNS: Final[tuple[str, ...]] = (
    "pitcher_id",            # str
    "outing_id",             # str — 이 경기에서 이 투수의 등판 단위 (game_id + appearance_idx)
    "pitch_seq",             # int >= 1 — 등판 내 누적 투구수
    "pitch_speed_kmh",       # int 60..170
    "pitch_type_idx",        # int 0..7 (FF/FB/SI/CT/SL/CB/CH/SF mapping)
    "inning",                # int >= 1
    "outs",                  # int 0..2
    "runners_on",            # int 0..3
    "batter_lineup_pos",     # int 1..9
    "allowed_baserunner",    # int 0/1 — supervised label (PA 종료 시 부여)
)

# Pitch type idx 매핑 (Java PitcherLimitFeatures 와 동일 순서)
PITCH_TYPE_INDEX: Final[dict[str, int]] = {
    "FF": 0, "FB": 1, "SI": 2, "CT": 3, "SL": 4, "CB": 5, "CH": 6, "SF": 7,
}


@dataclass(frozen=True)
class SchemaError:
    row: int
    column: str
    reason: str


def validate(df: pd.DataFrame) -> list[SchemaError]:
    errors: list[SchemaError] = []

    missing = [c for c in REQUIRED_COLUMNS if c not in df.columns]
    for col in missing:
        errors.append(SchemaError(row=-1, column=col, reason="missing required column"))
    if missing:
        return errors

    int_ranges = [
        ("pitch_seq", 1, 200),
        ("pitch_speed_kmh", 60, 170),
        ("pitch_type_idx", 0, 7),
        ("inning", 1, 20),
        ("outs", 0, 2),
        ("runners_on", 0, 3),
        ("batter_lineup_pos", 1, 9),
        ("allowed_baserunner", 0, 1),
    ]
    for idx, row in df.iterrows():
        for col, lo, hi in int_ranges:
            try:
                v = int(row[col])
            except (TypeError, ValueError):
                errors.append(SchemaError(int(idx), col, f"not int: {row[col]!r}"))
                continue
            if not (lo <= v <= hi):
                errors.append(SchemaError(int(idx), col, f"out of [{lo},{hi}]: {v}"))
    return errors
