package com.inplay.collector.robots;

import java.net.URI;

public interface RobotsGuard {

    boolean allowed(URI url, String userAgent);

    default void enforce(URI url, String userAgent) {
        if (!allowed(url, userAgent)) {
            throw new RobotsViolationException(url, userAgent);
        }
    }
}
