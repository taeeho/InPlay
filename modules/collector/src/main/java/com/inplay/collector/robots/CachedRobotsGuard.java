package com.inplay.collector.robots;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public final class CachedRobotsGuard implements RobotsGuard {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final RestClient restClient;
    private final Cache<String, RobotsTxtRule> cache;

    public CachedRobotsGuard(RestClient restClient) {
        this(restClient, DEFAULT_TTL);
    }

    public CachedRobotsGuard(RestClient restClient, Duration ttl) {
        this.restClient = Objects.requireNonNull(restClient, "restClient required");
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Objects.requireNonNull(ttl, "ttl required"))
                .maximumSize(256)
                .build();
    }

    @Override
    public boolean allowed(URI url, String userAgent) {
        Objects.requireNonNull(url, "url required");
        Objects.requireNonNull(userAgent, "userAgent required");
        String origin = origin(url);
        RobotsTxtRule rule = cache.get(origin, key -> fetch(url));
        String path = url.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        return rule.allowed(path, userAgent);
    }

    private RobotsTxtRule fetch(URI url) {
        URI robots = URI.create(origin(url) + "/robots.txt");
        try {
            String body = restClient.get().uri(robots).retrieve().body(String.class);
            return body == null ? RobotsTxtRule.allowAll() : RobotsTxtRule.parse(body);
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.is4xxClientError()) {
                return RobotsTxtRule.allowAll();
            }
            throw ex;
        }
    }

    private static String origin(URI url) {
        String scheme = url.getScheme();
        String host = url.getHost();
        int port = url.getPort();
        StringBuilder sb = new StringBuilder().append(scheme).append("://").append(host);
        if (port > 0) {
            sb.append(':').append(port);
        }
        return sb.toString();
    }
}
