"""PyTorch LSTM pitcher-limit classifier 학습. CSV → sequence → train → metrics.

작은 LSTM (hidden=16, 1 layer)으로 베타 데이터셋(~수백 outing)에서도 학습 가능.
실제 KBO 데이터 충분히 모이면 hidden ↑, dropout ↑, multi-layer 확장.

Split semantics (Codex 검토 반영):
- 현재 outing 단위 split → "outing leak" 방지 (같은 등판의 pitch가 train/test 동시 출현 차단).
- "pitcher leak"(같은 투수의 다른 등판이 train/test에 분산)은 아직 차단 X.
  pitcher 정체성 자체가 feature가 아니라 베타에서는 outing split이면 충분하지만,
  W5 본작업 시 본격 평가에서는 pitcher_id 단위 group split 옵션 추가 검토
  (sklearn GroupKFold + pitcher_id 그룹).

Drift 가드: 정규화 상수(speed-130/30 등)는 Python features.py와 Java PitcherLimitFeatures
양쪽에 하드코딩. drift 방지 위해 W5 본작업에서 model_metadata.json export 검토.
"""
from __future__ import annotations

import argparse
import json
from dataclasses import asdict, dataclass
from pathlib import Path

import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from sklearn.metrics import accuracy_score, log_loss, roc_auc_score

from . import data_schema, features


class PitcherLimitLSTM(nn.Module):
    def __init__(self, n_features: int, hidden: int = 16):
        super().__init__()
        self.lstm = nn.LSTM(input_size=n_features, hidden_size=hidden, batch_first=True)
        self.head = nn.Linear(hidden, 1)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # x: (B, T, F). 마지막 timestep 출력 사용.
        out, _ = self.lstm(x)
        last = out[:, -1, :]
        return torch.sigmoid(self.head(last)).squeeze(-1)


@dataclass
class TrainResult:
    n_train: int
    n_holdout: int
    holdout_accuracy: float
    holdout_log_loss: float
    holdout_auc: float
    epochs: int
    model_path: str


def train(
    csv_path: Path,
    out_dir: Path,
    max_seq_len: int = 120,
    epochs: int = 40,
    lr: float = 1e-3,
    hidden: int = 16,
    holdout_ratio: float = 0.25,
    seed: int = 42,
) -> TrainResult:
    df_raw = pd.read_csv(csv_path)
    errors = data_schema.validate(df_raw)
    if errors:
        raise ValueError(f"schema errors: {errors[:5]}{' ...' if len(errors) > 5 else ''}")

    X, y, outings = features.build_sequences(df_raw, max_len=max_seq_len)
    if len(outings) < 4:
        raise ValueError(f"need >= 4 distinct outing_ids, have {len(outings)}")

    # outing 단위 split — 같은 등판의 pitch가 train/test에 동시에 들어가는 'outing leak' 방지.
    # NOTE: pitcher_id leak (같은 투수의 다른 등판이 양쪽에 분산)은 아직 차단 X — W5 본작업 시 GroupKFold 검토.
    holdout_n = max(int(round(len(outings) * holdout_ratio)), 1)
    X_train, X_holdout = X[:-holdout_n], X[-holdout_n:]
    y_train, y_holdout = y[:-holdout_n], y[-holdout_n:]

    torch.manual_seed(seed)
    np.random.seed(seed)
    model = PitcherLimitLSTM(n_features=len(features.FEATURE_COLUMNS), hidden=hidden)
    optimizer = torch.optim.Adam(model.parameters(), lr=lr)
    loss_fn = nn.BCELoss()

    X_train_t = torch.from_numpy(X_train)
    y_train_t = torch.from_numpy(y_train).float()

    for _ in range(epochs):
        model.train()
        optimizer.zero_grad()
        pred = model(X_train_t)
        loss = loss_fn(pred, y_train_t)
        loss.backward()
        optimizer.step()

    model.eval()
    with torch.no_grad():
        proba = model(torch.from_numpy(X_holdout)).numpy()
    preds = (proba >= 0.5).astype(int)

    out_dir.mkdir(parents=True, exist_ok=True)
    model_path = out_dir / "pitcher_limit_lstm.pt"
    torch.save({"state_dict": model.state_dict(), "hidden": hidden,
                "n_features": len(features.FEATURE_COLUMNS), "max_seq_len": max_seq_len},
               str(model_path))

    result = TrainResult(
        n_train=int(len(X_train)),
        n_holdout=int(len(X_holdout)),
        holdout_accuracy=float(accuracy_score(y_holdout, preds)),
        holdout_log_loss=float(log_loss(y_holdout, proba.clip(1e-7, 1 - 1e-7), labels=[0, 1])),
        holdout_auc=float(roc_auc_score(y_holdout, proba)) if len(set(y_holdout.tolist())) > 1 else float("nan"),
        epochs=epochs,
        model_path=str(model_path),
    )

    (out_dir / "metrics.json").write_text(json.dumps(asdict(result), indent=2))
    return result


def _cli() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", type=Path, required=True)
    parser.add_argument("--out", type=Path, default=Path("runs/pitcher_limit/v1"))
    parser.add_argument("--epochs", type=int, default=40)
    args = parser.parse_args()
    result = train(args.csv, args.out, epochs=args.epochs)
    print(json.dumps(asdict(result), indent=2))


if __name__ == "__main__":
    _cli()
