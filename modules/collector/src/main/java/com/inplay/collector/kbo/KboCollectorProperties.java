package com.inplay.collector.kbo;

import java.net.URI;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inplay.collector.kbo")
public record KboCollectorProperties(URI scheduleUrl, Mode mode) {

    public KboCollectorProperties(URI scheduleUrl, Mode mode) {
        this.scheduleUrl = Objects.requireNonNull(scheduleUrl, "scheduleUrl required");
        this.mode = (mode == null) ? Mode.HTTP : mode;
    }

    public enum Mode {
        HTTP,
        HEADLESS
    }
}
