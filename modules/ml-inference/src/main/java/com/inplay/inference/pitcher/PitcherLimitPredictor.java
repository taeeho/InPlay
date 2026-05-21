package com.inplay.inference.pitcher;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * ONNX Runtime Java로 LSTM 투수 한계 모델 추론.
 *
 * <p>모델 산출 위치(예정): {@code modules/ml-inference/src/main/resources/models/v{n}/pitcher_limit.onnx}.
 * trainer Python(PyTorch → torch.onnx.export)에서 export. parity sample JSON과 함께 commit.
 *
 * <p>출력: 다음 plate appearance에서 baserunner 허용 확률 ∈ [0,1].
 * 호출자(decision)가 threshold + WPA context와 결합해 "투수 교체 push" 의사결정.
 */
public final class PitcherLimitPredictor implements AutoCloseable {

    private static final String INPUT_NAME = "input";

    private final OrtEnvironment env;
    private final OrtSession session;

    public PitcherLimitPredictor(Path onnxPath) {
        Objects.requireNonNull(onnxPath, "onnxPath required");
        this.env = OrtEnvironment.getEnvironment();
        try {
            this.session = env.createSession(onnxPath.toString(), new OrtSession.SessionOptions());
        } catch (OrtException e) {
            throw new IllegalStateException("failed to load ONNX model: " + onnxPath, e);
        }
    }

    public double predict(PitcherLimitFeatures features) {
        Objects.requireNonNull(features, "features required");
        float[][][] input = features.toTensor();
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, input);
             OrtSession.Result result = session.run(Map.of(INPUT_NAME, tensor))) {
            return extractProba(result);
        } catch (OrtException e) {
            throw new IllegalStateException("ONNX inference failed", e);
        }
    }

    private static double extractProba(OrtSession.Result result) throws OrtException {
        for (Map.Entry<String, OnnxValue> entry : result) {
            Object value = entry.getValue().getValue();
            if (value instanceof float[] arr && arr.length >= 1) {
                return arr[0];
            }
            if (value instanceof float[][] arr2d && arr2d.length == 1 && arr2d[0].length >= 1) {
                return arr2d[0][0];
            }
        }
        throw new IllegalStateException("expected float / (1,1) output, not found");
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            throw new IllegalStateException("failed to close ONNX session", e);
        }
    }
}
