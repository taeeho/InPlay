"""학습된 LightGBM clutch Booster → ONNX. Python vs ONNX parity 검증."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import lightgbm as lgb
import numpy as np
import onnxruntime as ort
from onnxmltools.convert import convert_lightgbm
from onnxmltools.convert.common.data_types import FloatTensorType

from . import features

PARITY_TOLERANCE = 1e-4


def export(booster_path: Path, onnx_path: Path, sample_path: Path) -> dict:
    booster = lgb.Booster(model_file=str(booster_path))
    n_features = len(features.FEATURE_COLUMNS)
    initial_type = [("input", FloatTensorType([None, n_features]))]
    onnx_model = convert_lightgbm(booster, initial_types=initial_type, zipmap=False)

    onnx_path.parent.mkdir(parents=True, exist_ok=True)
    with open(onnx_path, "wb") as f:
        f.write(onnx_model.SerializeToString())
    sha = hashlib.sha256(onnx_path.read_bytes()).hexdigest()

    sample_input = _deterministic_sample(n_features, count=5)

    lgbm_proba = np.asarray(booster.predict(sample_input)).reshape(-1)

    sess = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    outputs = sess.run(None, {"input": sample_input.astype(np.float32)})
    onnx_proba = _extract_proba(outputs)

    if not np.allclose(lgbm_proba, onnx_proba, atol=PARITY_TOLERANCE):
        diff = float(np.max(np.abs(lgbm_proba - onnx_proba)))
        raise RuntimeError(
            f"Python lgbm vs ONNX parity diff {diff:.6f} > {PARITY_TOLERANCE}"
        )

    sample = {
        "feature_columns": list(features.FEATURE_COLUMNS),
        "input": sample_input.tolist(),
        "lgbm_proba_class1": lgbm_proba.tolist(),
        "onnx_proba_class1": onnx_proba.tolist(),
        "sha256": sha,
    }
    sample_path.write_text(json.dumps(sample, indent=2))
    return sample


def _extract_proba(outputs) -> np.ndarray:
    for out in outputs:
        arr = np.asarray(out)
        if arr.ndim == 2 and arr.shape[1] == 2:
            return arr[:, 1]
    arr = np.asarray(outputs[-1])
    return arr[:, -1] if arr.ndim == 2 else arr


def _deterministic_sample(n_features: int, count: int) -> np.ndarray:
    rng = np.random.default_rng(seed=42)
    return rng.uniform(0.0, 1.0, size=(count, n_features))


def _cli() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--booster", type=Path, required=True)
    parser.add_argument("--onnx", type=Path, required=True)
    parser.add_argument("--sample", type=Path, required=True)
    args = parser.parse_args()
    result = export(args.booster, args.onnx, args.sample)
    print(json.dumps({"sha256": result["sha256"]}, indent=2))


if __name__ == "__main__":
    _cli()
