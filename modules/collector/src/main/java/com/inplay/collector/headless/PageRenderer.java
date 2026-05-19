package com.inplay.collector.headless;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public interface PageRenderer {

    String render(URI url, RenderOptions options);

    record RenderOptions(
            String userAgent,
            List<String> abortPatterns,
            Duration timeout,
            String waitForSelector) {

        public RenderOptions {
            Objects.requireNonNull(userAgent, "userAgent required");
            Objects.requireNonNull(abortPatterns, "abortPatterns required");
            Objects.requireNonNull(timeout, "timeout required");
            abortPatterns = List.copyOf(abortPatterns);
            if (timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("timeout must be positive");
            }
        }
    }
}
