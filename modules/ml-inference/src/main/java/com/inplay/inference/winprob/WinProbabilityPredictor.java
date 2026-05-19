package com.inplay.inference.winprob;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * ONNX Runtime Java로 LightGBM 승률 모델 추론.
 *
 * <p>ONNX 모델 산출 위치(예정): {@code modules/ml-inference/src/main/resources/models/v{n}/winprob.onnx}.
 * 모델은 trainer Python(LightGBM → onnxmltools)에서 export. parity sample JSON과 함께 commit.
 */
public final class WinProbabilityPredictor implements AutoCloseable {

    private static final String INPUT_NAME = "input";

    private final OrtEnvironment env;
    private final OrtSession session;

    public WinProbabilityPredictor(Path onnxPath) {
        Objects.requireNonNull(onnxPath, "onnxPath required");
        this.env = OrtEnvironment.getEnvironment();
        try {
            this.session = env.createSession(onnxPath.toString(), new OrtSession.SessionOptions());
        } catch (OrtException e) {
            throw new IllegalStateException("failed to load ONNX model: " + onnxPath, e);
        }
    }

    public double predict(WinProbabilityFeatures features) {
        Objects.requireNonNull(features, "features required");
        float[][] input = new float[][]{features.toFloatArray()};
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, input);
             OrtSession.Result result = session.run(Map.of(INPUT_NAME, tensor))) {
            return extractClass1Probability(result);
        } catch (OrtException e) {
            throw new IllegalStateException("ONNX inference failed", e);
        }
    }

    private static double extractClass1Probability(OrtSession.Result result) throws OrtException {
        for (Map.Entry<String, OnnxValue> entry : result) {
            Object value = entry.getValue().getValue();
            if (value instanceof float[][] arr && arr.length == 1 && arr[0].length == 2) {
                return arr[0][1];
            }
        }
        throw new IllegalStateException("expected (1,2) float output (class 0/1 prob), not found");
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            throw new IllegalStateException("failed to close ONNX session", e);
        }
        // OrtEnvironment는 process-wide singleton — close X.
    }
}
