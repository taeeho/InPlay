import pandas as pd

from win_prob import data_schema


def test_validate_accepts_clean_data():
    df = pd.DataFrame([
        {"season": 2024, "date": "2024-04-01", "home_team": "HH",
         "away_team": "LG", "home_score": 5, "away_score": 3},
    ])
    assert data_schema.validate(df) == []


def test_validate_detects_missing_column():
    df = pd.DataFrame([{"season": 2024, "date": "2024-04-01"}])
    errors = data_schema.validate(df)
    assert any(e.column == "home_team" for e in errors)


def test_validate_rejects_unknown_team():
    df = pd.DataFrame([
        {"season": 2024, "date": "2024-04-01", "home_team": "ZZZ",
         "away_team": "LG", "home_score": 5, "away_score": 3},
    ])
    errors = data_schema.validate(df)
    assert any("unknown team code" in e.reason for e in errors)


def test_validate_rejects_home_equals_away():
    df = pd.DataFrame([
        {"season": 2024, "date": "2024-04-01", "home_team": "HH",
         "away_team": "HH", "home_score": 5, "away_score": 3},
    ])
    errors = data_schema.validate(df)
    assert any("home == away" in e.reason for e in errors)


def test_label_home_win():
    df = pd.DataFrame([
        {"home_score": 5, "away_score": 3},
        {"home_score": 1, "away_score": 4},
        {"home_score": 3, "away_score": 3},
    ])
    labels = data_schema.label_home_win(df)
    assert labels.iloc[0] == 1
    assert labels.iloc[1] == 0
    assert pd.isna(labels.iloc[2])
