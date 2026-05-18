package com.inplay.collector.robots;

import java.net.URI;

public class RobotsViolationException extends RuntimeException {
    private final URI url;
    private final String userAgent;

    public RobotsViolationException(URI url, String userAgent) {
        super("robots.txt disallows " + userAgent + " -> " + url);
        this.url = url;
        this.userAgent = userAgent;
    }

    public URI url() {
        return url;
    }

    public String userAgent() {
        return userAgent;
    }
}
