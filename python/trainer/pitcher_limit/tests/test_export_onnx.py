from pathlib import Path

import numpy as np
import onnxruntime as ort

from pitcher_limit import export_onnx, train_lstm


def test_export_runs_parity_check(tmp_path: Path):
    csv = Path(__file__).parent.parent / "fixtures" / "sample_pitches.csv"
    out = tmp_path / "run"
    result = train_lstm.train(csv, out, epochs=5, max_seq_len=20)

    onnx_path = out / "pitcher_limit.onnx"
    sample_path = out / "parity_sample.json"
    sample = export_onnx.export(Path(result.model_path), onnx_path, sample_path)

    assert onnx_path.exists()
    assert sample["sha256"]
    assert sample["max_seq_len"] == 20
    assert len(sample["input"]) == len(sample["onnx_proba_class1"])
    assert len(sample["torch_proba_class1"]) == len(sample["onnx_proba_class1"])

    sess = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    arr = np.asarray(sample["input"], dtype=np.float32)
    outputs = sess.run(None, {"input": arr})
    proba = outputs[0].reshape(-1)
    np.testing.assert_allclose(proba, sample["onnx_proba_class1"], atol=1e-6)
