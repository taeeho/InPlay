import json
from pathlib import Path

from pitcher_limit import train_lstm


def test_train_runs_on_fixture(tmp_path: Path):
    csv = Path(__file__).parent.parent / "fixtures" / "sample_pitches.csv"
    out = tmp_path / "run"
    result = train_lstm.train(csv, out, epochs=5, max_seq_len=20)

    assert result.n_train > 0
    assert result.n_holdout >= 1
    assert 0.0 <= result.holdout_accuracy <= 1.0
    assert (out / "pitcher_limit_lstm.pt").exists()
    assert (out / "metrics.json").exists()

    metrics = json.loads((out / "metrics.json").read_text())
    assert metrics["holdout_accuracy"] == result.holdout_accuracy
