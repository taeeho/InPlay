package com.inplay.api.clutch;

import com.inplay.decision.clutch.ClutchDetector;
import com.inplay.decision.clutch.ClutchFeatureBuilder;
import com.inplay.inference.clutch.ClutchPredictor;
import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clutch ONNX 모델 산출물이 존재할 때만 predictor 로드, 없으면 ClutchDetector를
 * MODEL_NOT_READY 모드(predictor=null)로 등록.
 */
@Configuration
public class ClutchPredictorConfig {

    private static final Logger log = LoggerFactory.getLogger(ClutchPredictorConfig.class);

    private ClutchPredictor predictor;

    @Bean
    public ClutchFeatureBuilder clutchFeatureBuilder() {
        return new ClutchFeatureBuilder();
    }

    @Bean
    public ClutchDetector clutchDetector(
            ClutchFeatureBuilder featureBuilder,
            ClutchProperties props,
            @Value("${inplay.ml.model-dir:modules/ml-inference/src/main/resources/models}") String modelDir) {
        Path onnx = Path.of(modelDir, "v1", "clutch.onnx");
        if (Files.exists(onnx)) {
            log.info("loading ONNX clutch model from {}", onnx);
            predictor = new ClutchPredictor(onnx);
        } else {
            log.info("ONNX clutch model not found at {} — clutch will run in 'MODEL_NOT_READY' mode", onnx);
        }
        return new ClutchDetector(featureBuilder, predictor, props.detectorThreshold());
    }

    @PreDestroy
    void closePredictor() {
        if (predictor != null) {
            predictor.close();
        }
    }
}
