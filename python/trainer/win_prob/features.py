"""Feature engineering — 경기 시점 직전까지의 통계만 사용 (시간 누설 방지)."""
from __future__ import annotations

from collections import defaultdict, deque
from typing import Deque

import numpy as np
import pandas as pd

FEATURE_COLUMNS: tuple[str, ...] = (
    "home_season_win_rate",
    "away_season_win_rate",
    "home_home_win_rate",
    "away_away_win_rate",
    "home_recent10_win_rate",
    "away_recent10_win_rate",
    "h2h_home_win_rate",
)


def build_features(games: pd.DataFrame) -> pd.DataFrame:
    """row 단위 누적 통계를 생성. games는 (season, date) 오름차순 정렬되어 있어야.

    반환: FEATURE_COLUMNS + label 컬럼이 추가된 새 DataFrame (원본 보존).
    무승부 row는 제외 (label NaN).
    """
    df = games.sort_values(["season", "date"]).reset_index(drop=True).copy()

    # 시즌 단위로 누적 통계 리셋
    team_g = defaultdict(int)       # 시즌 경기 수
    team_w = defaultdict(int)       # 시즌 승
    team_home_g = defaultdict(int)
    team_home_w = defaultdict(int)
    team_away_g = defaultdict(int)
    team_away_w = defaultdict(int)
    recent: dict[str, Deque[int]] = defaultdict(lambda: deque(maxlen=10))
    h2h_g: dict[tuple[str, str], int] = defaultdict(int)   # (home, away) home승률용
    h2h_home_w: dict[tuple[str, str], int] = defaultdict(int)

    current_season: int | None = None

    rows = []
    for _, row in df.iterrows():
        season = int(row["season"])
        home = str(row["home_team"])
        away = str(row["away_team"])

        if current_season != season:
            team_g.clear(); team_w.clear()
            team_home_g.clear(); team_home_w.clear()
            team_away_g.clear(); team_away_w.clear()
            recent.clear()
            h2h_g.clear(); h2h_home_w.clear()
            current_season = season

        feat = {
            "home_season_win_rate": _ratio(team_w[home], team_g[home]),
            "away_season_win_rate": _ratio(team_w[away], team_g[away]),
            "home_home_win_rate": _ratio(team_home_w[home], team_home_g[home]),
            "away_away_win_rate": _ratio(team_away_w[away], team_away_g[away]),
            "home_recent10_win_rate": _ratio(sum(recent[home]), len(recent[home])),
            "away_recent10_win_rate": _ratio(sum(recent[away]), len(recent[away])),
            "h2h_home_win_rate": _ratio(h2h_home_w[(home, away)], h2h_g[(home, away)]),
        }

        hs = int(row["home_score"]); as_ = int(row["away_score"])
        if hs == as_:
            label = np.nan
        elif hs > as_:
            label = 1
            team_w[home] += 1; team_home_w[home] += 1
            recent[home].append(1); recent[away].append(0)
            h2h_home_w[(home, away)] += 1
        else:
            label = 0
            team_w[away] += 1; team_away_w[away] += 1
            recent[away].append(1); recent[home].append(0)

        team_g[home] += 1; team_g[away] += 1
        team_home_g[home] += 1; team_away_g[away] += 1
        h2h_g[(home, away)] += 1

        rows.append({**feat, "label": label, "season": season, "date": row["date"]})

    return pd.DataFrame(rows)


def _ratio(num: int, denom: int) -> float:
    return float(num) / float(denom) if denom else 0.5
