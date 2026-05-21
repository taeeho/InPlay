from pathlib import Path

import numpy as np
import onnxruntime as ort

from clutch import export_onnx, train_lgbm


def test_export_runs_parity_check(tmp_path: Path):
    csv = Path(__file__).parent.parent / "fixtures" / "sample_events.csv"
    out = tmp_path / "run"
    result = train_lgbm.train(csv, out, holdout_ratio=0.25, n_estimators=20)

    onnx_path = out / "clutch.onnx"
    sample_path = out / "parity_sample.json"
    sample = export_onnx.export(Path(result.model_path), onnx_path, sample_path)

    assert onnx_path.exists()
    assert sample["sha256"]
    assert len(sample["input"]) == len(sample["onnx_proba_class1"])
    assert len(sample["lgbm_proba_class1"]) == len(sample["onnx_proba_class1"])

    sess = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    arr = np.asarray(sample["input"], dtype=np.float32)
    outputs = sess.run(None, {"input": arr})
    proba = export_onnx._extract_proba(outputs)
    np.testing.assert_allclose(proba, sample["onnx_proba_class1"], atol=1e-6)
