"""Clutch feature engineering — LiveEvent + WPA 컨텍스트만 사용 (시간 누설 방지).

모든 입력은 'AFTER the play' 컨벤션 (LiveEvent.score / outs / wpa_after).
"""
from __future__ import annotations

import pandas as pd

FEATURE_COLUMNS: tuple[str, ...] = (
    "wpa_change_abs",      # |we_home_after - we_home_before|
    "we_balance",          # 1 - 2*|we_home_after - 0.5|  (균형 클수록 1, 한쪽 우세할수록 0)
    "inning_progress",     # min(inning, 9) / 9  (후반 가중)
    "score_margin_norm",   # min(score_margin_abs, 5) / 5  (접전 가중)
    "runners_on_norm",     # runners_on / 3
    "outs_norm",           # outs / 3
    "leverage_proxy",      # wpa_change_abs × inning_progress × (1 + 0.5*runners_on_norm)
)


def build_features(events: pd.DataFrame) -> pd.DataFrame:
    """row 단위 feature 생성. 시간 누설 X — 각 row의 컬럼만 참조."""
    df = events.copy()

    wpa_change_abs = (df["we_home_after"] - df["we_home_before"]).abs()
    we_balance = 1.0 - 2.0 * (df["we_home_after"] - 0.5).abs()
    inning_progress = df["inning"].clip(upper=9) / 9.0
    score_margin_norm = df["score_margin_abs"].clip(upper=5) / 5.0
    runners_on_norm = df["runners_on"] / 3.0
    outs_norm = df["outs"] / 3.0
    leverage_proxy = wpa_change_abs * inning_progress * (1.0 + 0.5 * runners_on_norm)

    out = pd.DataFrame({
        "game_id": df["game_id"].astype(str),  # split key — inference에서는 drop
        "wpa_change_abs": wpa_change_abs,
        "we_balance": we_balance,
        "inning_progress": inning_progress,
        "score_margin_norm": score_margin_norm,
        "runners_on_norm": runners_on_norm,
        "outs_norm": outs_norm,
        "leverage_proxy": leverage_proxy,
        "clutch": df["clutch"].astype(int),
    })
    return out
