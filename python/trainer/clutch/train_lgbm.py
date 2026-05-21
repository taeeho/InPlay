"""LightGBM clutch classifier 학습. CSV → feature → 학습 → metrics."""
from __future__ import annotations

import argparse
import json
from dataclasses import asdict, dataclass
from pathlib import Path

import lightgbm as lgb
import pandas as pd
from sklearn.metrics import accuracy_score, log_loss, precision_score, recall_score, roc_auc_score

from . import data_schema, features


@dataclass
class TrainResult:
    n_train: int
    n_holdout: int
    holdout_accuracy: float
    holdout_precision: float
    holdout_recall: float
    holdout_log_loss: float
    holdout_auc: float
    n_estimators: int
    model_path: str


def train(
    csv_path: Path,
    out_dir: Path,
    holdout_ratio: float = 0.2,
    n_estimators: int = 150,
    learning_rate: float = 0.05,
    num_leaves: int = 11,
    seed: int = 42,
    threshold: float = 0.5,
) -> TrainResult:
    """Train clutch classifier with game_id-based train/holdout split.

    Why game_id split (NOT row split): 같은 게임의 LiveEvent들은 시퀀스 상관관계가
    강해서 row-shuffled split은 holdout 메트릭을 과대평가함. 게임 단위 leak-free.

    threshold: precision/recall 보고용 cutoff. 0.5 기본, calibration은 W4 라벨 수집 후.
    """
    df_raw = pd.read_csv(csv_path)
    errors = data_schema.validate(df_raw)
    if errors:
        raise ValueError(f"schema errors: {errors[:5]}{' ...' if len(errors) > 5 else ''}")

    df_feat = features.build_features(df_raw).reset_index(drop=True)
    if len(df_feat) < 20:
        raise ValueError(f"insufficient rows: have {len(df_feat)}, need >= 20")

    game_ids = df_feat["game_id"].drop_duplicates().tolist()
    if len(game_ids) < 2:
        raise ValueError(f"need >= 2 distinct game_ids for split, have {len(game_ids)}")
    holdout_games = max(int(round(len(game_ids) * holdout_ratio)), 1)
    holdout_ids = set(game_ids[-holdout_games:])

    train_df = df_feat[~df_feat["game_id"].isin(holdout_ids)]
    holdout_df = df_feat[df_feat["game_id"].isin(holdout_ids)]

    feat_cols = list(features.FEATURE_COLUMNS)
    X_train = train_df[feat_cols].to_numpy()
    y_train = train_df["clutch"].to_numpy()
    X_holdout = holdout_df[feat_cols].to_numpy()
    y_holdout = holdout_df["clutch"].to_numpy()

    model = lgb.LGBMClassifier(
        objective="binary",
        n_estimators=n_estimators,
        learning_rate=learning_rate,
        num_leaves=num_leaves,
        random_state=seed,
        verbosity=-1,
    )
    model.fit(X_train, y_train)

    proba = model.predict_proba(X_holdout)[:, 1]
    preds = (proba >= threshold).astype(int)

    out_dir.mkdir(parents=True, exist_ok=True)
    model_path = out_dir / "clutch_lgbm.txt"
    model.booster_.save_model(str(model_path))

    result = TrainResult(
        n_train=int(len(train_df)),
        n_holdout=int(len(holdout_df)),
        holdout_accuracy=float(accuracy_score(y_holdout, preds)),
        holdout_precision=float(precision_score(y_holdout, preds, zero_division=0)),
        holdout_recall=float(recall_score(y_holdout, preds, zero_division=0)),
        holdout_log_loss=float(log_loss(y_holdout, proba, labels=[0, 1])),
        holdout_auc=float(roc_auc_score(y_holdout, proba)) if len(set(y_holdout)) > 1 else float("nan"),
        n_estimators=n_estimators,
        model_path=str(model_path),
    )

    (out_dir / "metrics.json").write_text(json.dumps(asdict(result), indent=2))
    return result


def _cli() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", type=Path, required=True)
    parser.add_argument("--out", type=Path, default=Path("runs/clutch/v1"))
    parser.add_argument("--holdout-ratio", type=float, default=0.2)
    args = parser.parse_args()
    result = train(args.csv, args.out, holdout_ratio=args.holdout_ratio)
    print(json.dumps(asdict(result), indent=2))


if __name__ == "__main__":
    _cli()
