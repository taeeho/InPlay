package com.inplay.api.brief;

import com.inplay.decision.brief.BriefGenerator;
import com.inplay.decision.brief.WinProbabilityFeatureBuilder;
import com.inplay.inference.winprob.WinProbabilityPredictor;
import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ONNX 모델 산출물 파일이 존재할 때만 predictor 로드. 없으면 null → BriefGenerator가 "모델 미준비" brief 생성.
 */
@Configuration
public class WinProbabilityPredictorConfig {

    private static final Logger log = LoggerFactory.getLogger(WinProbabilityPredictorConfig.class);

    private WinProbabilityPredictor predictor;

    @Bean
    public WinProbabilityFeatureBuilder featureBuilder() {
        return new WinProbabilityFeatureBuilder();
    }

    @Bean
    public BriefGenerator briefGenerator(
            WinProbabilityFeatureBuilder featureBuilder,
            @Value("${inplay.ml.model-dir:modules/ml-inference/src/main/resources/models}") String modelDir) {
        Path onnx = Path.of(modelDir, "v1", "winprob.onnx");
        if (Files.exists(onnx)) {
            log.info("loading ONNX win-prob model from {}", onnx);
            predictor = new WinProbabilityPredictor(onnx);
        } else {
            log.info("ONNX model not found at {} — brief will run in 'model 미준비' mode", onnx);
        }
        return new BriefGenerator(featureBuilder, predictor);
    }

    @PreDestroy
    void closePredictor() {
        if (predictor != null) {
            predictor.close();
        }
    }
}
