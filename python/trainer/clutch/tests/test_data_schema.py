import pandas as pd

from clutch import data_schema


def _good_row(**overrides):
    base = {
        "game_id": "g1", "event_seq": 0,
        "we_home_after": 0.55, "we_home_before": 0.50,
        "inning": 5, "score_margin_abs": 1, "runners_on": 1, "outs": 1,
        "clutch": 0,
    }
    base.update(overrides)
    return base


def test_validate_accepts_clean_data():
    df = pd.DataFrame([_good_row()])
    assert data_schema.validate(df) == []


def test_validate_detects_missing_column():
    df = pd.DataFrame([{"game_id": "g1", "event_seq": 0}])
    errors = data_schema.validate(df)
    assert any(e.column == "we_home_after" for e in errors)


def test_validate_rejects_out_of_range_we():
    df = pd.DataFrame([_good_row(we_home_after=1.5)])
    errors = data_schema.validate(df)
    assert any(e.column == "we_home_after" for e in errors)


def test_validate_rejects_out_of_range_runners():
    df = pd.DataFrame([_good_row(runners_on=4)])
    errors = data_schema.validate(df)
    assert any(e.column == "runners_on" for e in errors)


def test_validate_rejects_non_binary_clutch_label():
    df = pd.DataFrame([_good_row(clutch=2)])
    errors = data_schema.validate(df)
    assert any(e.column == "clutch" for e in errors)
