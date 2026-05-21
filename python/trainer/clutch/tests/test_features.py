import pandas as pd

from clutch import features


def _events():
    return pd.DataFrame([
        # 초반 평범
        {"game_id": "g1", "event_seq": 0, "we_home_after": 0.50, "we_home_before": 0.50,
         "inning": 1, "score_margin_abs": 0, "runners_on": 0, "outs": 0, "clutch": 0},
        # 후반 접전 큰 변화
        {"game_id": "g1", "event_seq": 1, "we_home_after": 0.30, "we_home_before": 0.65,
         "inning": 8, "score_margin_abs": 1, "runners_on": 2, "outs": 1, "clutch": 1},
    ])


def test_feature_columns_are_stable():
    expected = (
        "wpa_change_abs", "we_balance", "inning_progress",
        "score_margin_norm", "runners_on_norm", "outs_norm", "leverage_proxy",
    )
    assert features.FEATURE_COLUMNS == expected


def test_wpa_change_is_absolute():
    df = features.build_features(_events())
    assert df["wpa_change_abs"].iloc[0] == 0.0
    assert abs(df["wpa_change_abs"].iloc[1] - 0.35) < 1e-9


def test_we_balance_higher_when_closer_to_half():
    df = features.build_features(_events())
    # row 0: we=0.5 → balance=1.0; row 1: we=0.30 → balance=1 - 0.4 = 0.6
    assert df["we_balance"].iloc[0] > df["we_balance"].iloc[1]


def test_inning_progress_caps_at_one():
    big = pd.DataFrame([{
        "game_id": "g1", "event_seq": 0, "we_home_after": 0.5, "we_home_before": 0.5,
        "inning": 12, "score_margin_abs": 0, "runners_on": 0, "outs": 0, "clutch": 0,
    }])
    assert features.build_features(big)["inning_progress"].iloc[0] == 1.0


def test_leverage_proxy_combines_swing_inning_runners():
    df = features.build_features(_events())
    early = df["leverage_proxy"].iloc[0]
    late = df["leverage_proxy"].iloc[1]
    assert late > early
