package com.inplay.inference.clutch;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * ONNX Runtime Java로 LightGBM clutch(결정적 순간) classifier 추론.
 *
 * <p>모델 산출 위치(예정): {@code modules/ml-inference/src/main/resources/models/v{n}/clutch.onnx}.
 * trainer Python(LightGBM → onnxmltools)에서 export. parity sample JSON과 함께 commit.
 *
 * <p>출력: 이벤트가 'clutch'(결정적 순간)일 확률 ∈ [0,1].
 * threshold(예: ≥ 0.7)는 호출자(decision/notify)가 결정 — `WpaCalculator`로 ΔWE 임계와
 * 함께 importance score를 만드는 게 W4 시나리오.
 */
public final class ClutchPredictor implements AutoCloseable {

    private static final String INPUT_NAME = "input";

    private final OrtEnvironment env;
    private final OrtSession session;

    public ClutchPredictor(Path onnxPath) {
        Objects.requireNonNull(onnxPath, "onnxPath required");
        this.env = OrtEnvironment.getEnvironment();
        try {
            this.session = env.createSession(onnxPath.toString(), new OrtSession.SessionOptions());
        } catch (OrtException e) {
            throw new IllegalStateException("failed to load ONNX model: " + onnxPath, e);
        }
    }

    public double predict(ClutchFeatures features) {
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
    }
}
