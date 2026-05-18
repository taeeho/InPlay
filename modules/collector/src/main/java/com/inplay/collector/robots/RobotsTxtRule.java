package com.inplay.collector.robots;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RobotsTxtRule(List<Group> groups) {

    public RobotsTxtRule {
        Objects.requireNonNull(groups, "groups required");
        groups = List.copyOf(groups);
    }

    public static RobotsTxtRule allowAll() {
        return new RobotsTxtRule(List.of());
    }

    public boolean allowed(String path, String userAgent) {
        Objects.requireNonNull(path, "path required");
        Objects.requireNonNull(userAgent, "userAgent required");
        Group matched = selectGroup(userAgent);
        if (matched == null) {
            return true;
        }
        return matched.allowedFor(path);
    }

    private Group selectGroup(String userAgent) {
        String uaLower = userAgent.toLowerCase();
        Group fallback = null;
        for (Group g : groups) {
            for (String ua : g.userAgents()) {
                if (ua.equals("*")) {
                    fallback = g;
                } else if (uaLower.contains(ua.toLowerCase())) {
                    return g;
                }
            }
        }
        return fallback;
    }

    public static RobotsTxtRule parse(String body) {
        Objects.requireNonNull(body, "body required");
        List<Group> groups = new ArrayList<>();
        List<String> uas = new ArrayList<>();
        List<String> disallows = new ArrayList<>();
        boolean inDirectives = false;

        for (String rawLine : body.split("\\R")) {
            String trimmedRaw = rawLine.trim();
            if (trimmedRaw.isEmpty()) {
                if (!uas.isEmpty()) {
                    groups.add(new Group(List.copyOf(uas), List.copyOf(disallows)));
                    uas.clear();
                    disallows.clear();
                    inDirectives = false;
                }
                continue;
            }
            if (trimmedRaw.startsWith("#")) {
                continue;
            }
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String field = line.substring(0, colon).trim().toLowerCase();
            String value = line.substring(colon + 1).trim();
            switch (field) {
                case "user-agent" -> {
                    if (inDirectives && !uas.isEmpty()) {
                        groups.add(new Group(List.copyOf(uas), List.copyOf(disallows)));
                        uas.clear();
                        disallows.clear();
                        inDirectives = false;
                    }
                    uas.add(value);
                }
                case "disallow" -> {
                    if (!value.isEmpty()) {
                        disallows.add(value);
                    }
                    inDirectives = true;
                }
                case "allow" -> inDirectives = true;
                default -> { /* sitemap, crawl-delay 등 무시 */ }
            }
        }
        if (!uas.isEmpty()) {
            groups.add(new Group(List.copyOf(uas), List.copyOf(disallows)));
        }
        return new RobotsTxtRule(List.copyOf(groups));
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    public record Group(List<String> userAgents, List<String> disallowPaths) {
        public Group {
            Objects.requireNonNull(userAgents, "userAgents required");
            Objects.requireNonNull(disallowPaths, "disallowPaths required");
            userAgents = List.copyOf(userAgents);
            disallowPaths = List.copyOf(disallowPaths);
        }

        public boolean allowedFor(String path) {
            for (String dis : disallowPaths) {
                if (path.startsWith(dis)) {
                    return false;
                }
            }
            return true;
        }
    }
}
