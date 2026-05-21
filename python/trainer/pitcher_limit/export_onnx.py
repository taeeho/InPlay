"""학습된 LSTM → ONNX. Python(torch) vs ONNX runtime parity 검증."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
import onnxruntime as ort
import torch

from . import features
from .train_lstm import PitcherLimitLSTM

PARITY_TOLERANCE = 1e-4


def export(checkpoint_path: Path, onnx_path: Path, sample_path: Path) -> dict:
    ckpt = torch.load(str(checkpoint_path), weights_only=True)
    model = PitcherLimitLSTM(n_features=ckpt["n_features"], hidden=ckpt["hidden"])
    model.load_state_dict(ckpt["state_dict"])
    model.eval()

    max_seq_len = int(ckpt["max_seq_len"])
    n_features = int(ckpt["n_features"])

    dummy = torch.zeros((1, max_seq_len, n_features), dtype=torch.float32)
    onnx_path.parent.mkdir(parents=True, exist_ok=True)
    torch.onnx.export(
        model, dummy, str(onnx_path),
        input_names=["input"], output_names=["proba"],
        dynamic_axes={"input": {0: "batch"}, "proba": {0: "batch"}},
        opset_version=14,
    )
    sha = hashlib.sha256(onnx_path.read_bytes()).hexdigest()

    sample_input = _deterministic_sample(max_seq_len, n_features, count=5)
    with torch.no_grad():
        torch_proba = model(torch.from_numpy(sample_input)).numpy().reshape(-1)

    sess = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    onnx_proba = sess.run(None, {"input": sample_input})[0].reshape(-1)

    if not np.allclose(torch_proba, onnx_proba, atol=PARITY_TOLERANCE):
        diff = float(np.max(np.abs(torch_proba - onnx_proba)))
        raise RuntimeError(
            f"Python torch vs ONNX parity diff {diff:.6f} > {PARITY_TOLERANCE}"
        )

    sample = {
        "feature_columns": list(features.FEATURE_COLUMNS),
        "max_seq_len": max_seq_len,
        "input": sample_input.tolist(),
        "torch_proba_class1": torch_proba.tolist(),
        "onnx_proba_class1": onnx_proba.tolist(),
        "sha256": sha,
    }
    sample_path.write_text(json.dumps(sample, indent=2))
    return sample


def _deterministic_sample(max_seq_len: int, n_features: int, count: int) -> np.ndarray:
    rng = np.random.default_rng(seed=42)
    return rng.uniform(0.0, 1.0, size=(count, max_seq_len, n_features)).astype(np.float32)


def _cli() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint", type=Path, required=True)
    parser.add_argument("--onnx", type=Path, required=True)
    parser.add_argument("--sample", type=Path, required=True)
    args = parser.parse_args()
    result = export(args.checkpoint, args.onnx, args.sample)
    print(json.dumps({"sha256": result["sha256"]}, indent=2))


if __name__ == "__main__":
    _cli()
