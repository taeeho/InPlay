from pathlib import Path

import pandas as pd

from pitcher_limit import features


def _df():
    return pd.read_csv(Path(__file__).parent.parent / "fixtures" / "sample_pitches.csv")


def test_feature_columns_are_stable():
    expected = (
        "pitch_seq_norm", "pitch_speed_norm", "pitch_type_norm",
        "inning_norm", "outs_norm", "runners_on_norm", "lineup_pos_norm",
    )
    assert features.FEATURE_COLUMNS == expected


def test_build_sequences_groups_by_outing():
    X, y, outings = features.build_sequences(_df(), max_len=120)
    assert len(outings) == 5  # o1..o5
    assert X.shape == (5, 120, 7)
    assert y.shape == (5,)


def test_padding_with_zero_when_shorter_than_max_len():
    X, _, _ = features.build_sequences(_df(), max_len=20)
    # o1 has 7 pitches → 13 leading zero rows
    o1_seq = X[0]
    assert (o1_seq[:13] == 0.0).all()
    assert (o1_seq[13] != 0.0).any()


def test_label_taken_from_last_pitch_of_outing():
    X, y, outings = features.build_sequences(_df(), max_len=120)
    # o1 마지막 pitch_seq=7 label=1, o5 마지막 label=0
    o1_idx = outings.index("o1")
    o5_idx = outings.index("o5")
    assert y[o1_idx] == 1
    assert y[o5_idx] == 0


def test_long_sequence_truncated_to_recent_window():
    # 200 pitch row 만들기, max_len=50 → 끝 50개만 남음
    rows = []
    for i in range(1, 201):
        rows.append({
            "pitcher_id": "px", "outing_id": "oX", "pitch_seq": i,
            "pitch_speed_kmh": 140, "pitch_type_idx": 0,
            "inning": min(1 + i // 25, 9), "outs": i % 3, "runners_on": 0,
            "batter_lineup_pos": ((i - 1) % 9) + 1, "allowed_baserunner": 1 if i == 200 else 0,
        })
    long_df = pd.DataFrame(rows)
    X, y, _ = features.build_sequences(long_df, max_len=50)
    assert X.shape == (1, 50, 7)
    assert y[0] == 1
