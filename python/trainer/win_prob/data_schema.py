"""CSV 스키마 정의 — 학습 데이터 입력 계약.

사용자는 어떤 source(Wikipedia/수기/공식 download 등)든 이 스키마로 CSV를 준비한다.
한 row = 한 경기. 시즌·날짜 순으로 정렬되어 있어야 한다 (시간 누설 방지).
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Final

import pandas as pd

# KBO 10팀 코드 (modules/core KboTeam enum과 동일)
KBO_TEAM_CODES: Final[frozenset[str]] = frozenset(
    {"HH", "LG", "KIA", "SSG", "KT", "KIWOOM", "NC", "SAMSUNG", "LOTTE", "HAN"}
)

REQUIRED_COLUMNS: Final[tuple[str, ...]] = (
    "season",        # int, e.g. 2024
    "date",          # YYYY-MM-DD
    "home_team",     # KBO team code
    "away_team",     # KBO team code
    "home_score",    # int >= 0
    "away_score",    # int >= 0
)


@dataclass(frozen=True)
class SchemaError:
    row: int
    column: str
    reason: str


def validate(df: pd.DataFrame) -> list[SchemaError]:
    """데이터 검증. 오류 목록 반환 (비어있으면 통과)."""
    errors: list[SchemaError] = []

    missing = [c for c in REQUIRED_COLUMNS if c not in df.columns]
    for col in missing:
        errors.append(SchemaError(row=-1, column=col, reason="missing required column"))
    if missing:
        return errors

    for idx, row in df.iterrows():
        for code_col in ("home_team", "away_team"):
            code = str(row[code_col])
            if code not in KBO_TEAM_CODES:
                errors.append(SchemaError(int(idx), code_col, f"unknown team code: {code}"))
        if row["home_team"] == row["away_team"]:
            errors.append(SchemaError(int(idx), "home_team", "home == away"))
        for score_col in ("home_score", "away_score"):
            try:
                score = int(row[score_col])
                if score < 0:
                    errors.append(SchemaError(int(idx), score_col, f"negative: {score}"))
            except (TypeError, ValueError):
                errors.append(SchemaError(int(idx), score_col, f"not int: {row[score_col]!r}"))

    return errors


def label_home_win(df: pd.DataFrame) -> pd.Series:
    """label: 1 if home won, 0 if away won. 무승부는 NaN (학습에서 drop)."""
    margin = df["home_score"].astype(int) - df["away_score"].astype(int)
    return margin.apply(lambda m: 1 if m > 0 else (0 if m < 0 else pd.NA)).astype("Int64")
