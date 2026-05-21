import pandas as pd

from pitcher_limit import data_schema


def _row(**overrides):
    base = {
        "pitcher_id": "p1", "outing_id": "o1", "pitch_seq": 1,
        "pitch_speed_kmh": 145, "pitch_type_idx": 0,
        "inning": 1, "outs": 0, "runners_on": 0,
        "batter_lineup_pos": 1, "allowed_baserunner": 0,
    }
    base.update(overrides)
    return base


def test_validate_accepts_clean_row():
    df = pd.DataFrame([_row()])
    assert data_schema.validate(df) == []


def test_validate_detects_missing_column():
    df = pd.DataFrame([{"pitcher_id": "p1"}])
    errors = data_schema.validate(df)
    assert any(e.column == "outing_id" for e in errors)


def test_validate_rejects_out_of_range_speed():
    df = pd.DataFrame([_row(pitch_speed_kmh=40)])
    errors = data_schema.validate(df)
    assert any(e.column == "pitch_speed_kmh" for e in errors)


def test_validate_rejects_invalid_lineup_pos():
    df = pd.DataFrame([_row(batter_lineup_pos=10)])
    errors = data_schema.validate(df)
    assert any(e.column == "batter_lineup_pos" for e in errors)


def test_pitch_type_index_covers_eight_codes():
    assert set(data_schema.PITCH_TYPE_INDEX.values()) == set(range(8))
