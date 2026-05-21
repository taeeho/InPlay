"""Clutch classifier 학습 데이터 입력 계약.

한 row = 한 LiveEvent + 본인 ride-along 라벨.
라벨 수집은 W3 데이터 게이트 통과 후 (5경기 ride-along) 진행 예정. 이 스키마는 그 fixture.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Final

import pandas as pd

REQUIRED_COLUMNS: Final[tuple[str, ...]] = (
    "game_id",            # str (e.g. 20260512HHLG)
    "event_seq",          # int, 0-based within game
    "we_home_after",      # float [0,1]  ← LiveEvent.wpa_after
    "we_home_before",     # float [0,1]  ← previous event's wpa_after (0.5 for game start)
    "inning",             # int >= 1
    "score_margin_abs",   # int >= 0
    "runners_on",         # int 0..3
    "outs",               # int 0..3
    "clutch",             # int 0/1  ← supervised label (본인 매김)
)


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

    for idx, row in df.iterrows():
        for col, lo, hi in (
            ("we_home_after", 0.0, 1.0),
            ("we_home_before", 0.0, 1.0),
        ):
            v = float(row[col])
            if not (lo <= v <= hi):
                errors.append(SchemaError(int(idx), col, f"out of [{lo},{hi}]: {v}"))
        for col, lo, hi in (
            ("inning", 1, 20),
            ("score_margin_abs", 0, 30),
            ("runners_on", 0, 3),
            ("outs", 0, 3),
            ("clutch", 0, 1),
        ):
            try:
                v = int(row[col])
            except (TypeError, ValueError):
                errors.append(SchemaError(int(idx), col, f"not int: {row[col]!r}"))
                continue
            if not (lo <= v <= hi):
                errors.append(SchemaError(int(idx), col, f"out of [{lo},{hi}]: {v}"))

    return errors
