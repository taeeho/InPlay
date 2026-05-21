package com.inplay.inference.clutch;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Java ONNX Runtime vs Python(LightGBM) parity. trainer가 {@code clutch/export_onnx.py}로 만든
 * 산출물(clutch.onnx + parity_sample.json)이 있을 때만 실행. 1e-4 tolerance.
 */
@EnabledIf("hasModelArtifacts")
class ClutchParityTest {

    private static final Path MODEL_DIR = Path.of("src/main/resources/models/v1");
    private static final Path ONNX_PATH = MODEL_DIR.resolve("clutch.onnx");
    private static final Path SAMPLE_PATH = MODEL_DIR.resolve("clutch_parity_sample.json");
    private static final double TOLERANCE = 1e-4;

    static boolean hasModelArtifacts() {
        return Files.exists(ONNX_PATH) && Files.exists(SAMPLE_PATH);
    }

    @Test
    void javaOnnxMatchesPythonExport() throws IOException {
        JsonNode sample = new ObjectMapper().readTree(SAMPLE_PATH.toFile());
        JsonNode inputs = sample.get("input");
        JsonNode expected = sample.get("onnx_proba_class1");

        try (var predictor = new ClutchPredictor(ONNX_PATH)) {
            for (int i = 0; i < inputs.size(); i++) {
                JsonNode row = inputs.get(i);
                double[] values = new double[row.size()];
                for (int j = 0; j < row.size(); j++) {
                    values[j] = row.get(j).asDouble();
                }
                var features = ClutchFeatures.fromArray(values);
                double actual = predictor.predict(features);
                double expectedValue = expected.get(i).asDouble();
                assertThat(actual)
                        .as("row %d parity (expected=%.6f, actual=%.6f)", i, expectedValue, actual)
                        .isCloseTo(expectedValue, org.assertj.core.data.Offset.offset(TOLERANCE));
            }
        }
    }
}
