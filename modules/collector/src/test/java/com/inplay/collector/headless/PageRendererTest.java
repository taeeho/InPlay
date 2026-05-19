package com.inplay.collector.headless;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageRendererTest {

    @Test
    void renderOptionsCopiesAbortPatterns() {
        List<String> patterns = new java.util.ArrayList<>(List.of("**/ws/**", "**/track/**"));
        var opts = new PageRenderer.RenderOptions("UA", patterns, Duration.ofSeconds(10), "#root");
        patterns.add("**/ads/**");
        assertThat(opts.abortPatterns()).containsExactly("**/ws/**", "**/track/**");
    }

    @Test
    void renderOptionsRejectsZeroTimeout() {
        assertThatThrownBy(() ->
                new PageRenderer.RenderOptions("UA", List.of(), Duration.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renderOptionsRejectsNegativeTimeout() {
        assertThatThrownBy(() ->
                new PageRenderer.RenderOptions("UA", List.of(), Duration.ofSeconds(-1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void waitForSelectorIsOptional() {
        var opts = new PageRenderer.RenderOptions("UA", List.of(), Duration.ofSeconds(10), null);
        assertThat(opts.waitForSelector()).isNull();
    }
}
