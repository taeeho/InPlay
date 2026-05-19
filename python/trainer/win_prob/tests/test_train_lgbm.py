import json
from pathlib import Path

from win_prob import train_lgbm


def test_train_runs_on_fixture(tmp_path: Path):
    csv = Path(__file__).parent.parent / "fixtures" / "sample_games.csv"
    out = tmp_path / "run"
    result = train_lgbm.train(csv, out, holdout_size=10, n_estimators=20)

    assert result.n_train > 0
    assert result.n_holdout == 10
    assert 0.0 <= result.holdout_accuracy <= 1.0
    assert (out / "winprob_lgbm.txt").exists()
    assert (out / "metrics.json").exists()

    metrics = json.loads((out / "metrics.json").read_text())
    assert metrics["holdout_accuracy"] == result.holdout_accuracy
