package com.inplay.inference.pitcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Java ONNX Runtime vs Python(PyTorch) parity. trainer {@code pitcher_limit/export_onnx.py}
 * 산출물(pitcher_limit.onnx + parity_sample.json)이 있을 때만 실행. 1e-4 tolerance.
 */
@EnabledIf("hasModelArtifacts")
class PitcherLimitParityTest {

    private static final Path MODEL_DIR = Path.of("src/main/resources/models/v1");
    private static final Path ONNX_PATH = MODEL_DIR.resolve("pitcher_limit.onnx");
    private static final Path SAMPLE_PATH = MODEL_DIR.resolve("pitcher_limit_parity_sample.json");
    private static final double TOLERANCE = 1e-4;

    static boolean hasModelArtifacts() {
        return Files.exists(ONNX_PATH) && Files.exists(SAMPLE_PATH);
    }

    @Test
    void javaOnnxMatchesPythonExport() throws IOException {
        JsonNode sample = new ObjectMapper().readTree(SAMPLE_PATH.toFile());
        int maxSeqLen = sample.get("max_seq_len").asInt();
        JsonNode inputs = sample.get("input");
        JsonNode expected = sample.get("onnx_proba_class1");

        try (var predictor = new PitcherLimitPredictor(ONNX_PATH)) {
            for (int i = 0; i < inputs.size(); i++) {
                JsonNode seq = inputs.get(i);
                List<PitchSnapshot> snapshots = new ArrayList<>();
                for (int t = 0; t < seq.size(); t++) {
                    JsonNode row = seq.get(t);
                    snapshots.add(new PitchSnapshot(
                            row.get(0).asDouble(), row.get(1).asDouble(), row.get(2).asDouble(),
                            row.get(3).asDouble(), row.get(4).asDouble(), row.get(5).asDouble(),
                            row.get(6).asDouble()));
                }
                var features = new PitcherLimitFeatures(maxSeqLen, snapshots);
                double actual = predictor.predict(features);
                double expectedValue = expected.get(i).asDouble();
                assertThat(actual)
                        .as("row %d parity (expected=%.6f, actual=%.6f)", i, expectedValue, actual)
                        .isCloseTo(expectedValue, org.assertj.core.data.Offset.offset(TOLERANCE));
            }
        }
    }
}
