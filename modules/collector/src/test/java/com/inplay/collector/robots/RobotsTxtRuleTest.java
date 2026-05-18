package com.inplay.collector.robots;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RobotsTxtRuleTest {

    @Test
    void allowAllWhenNoGroups() {
        RobotsTxtRule rule = RobotsTxtRule.parse("");
        assertThat(rule.allowed("/anything", "inplay/0.1")).isTrue();
    }

    @Test
    void wildcardDisallowBlocksMatchingPrefix() {
        String body = """
                User-agent: *
                Disallow: /private
                """;
        RobotsTxtRule rule = RobotsTxtRule.parse(body);
        assertThat(rule.allowed("/private/data", "inplay/0.1")).isFalse();
        assertThat(rule.allowed("/public", "inplay/0.1")).isTrue();
    }

    @Test
    void specificUserAgentOverridesWildcard() {
        String body = """
                User-agent: *
                Disallow: /

                User-agent: inplay
                Disallow: /private
                """;
        RobotsTxtRule rule = RobotsTxtRule.parse(body);
        assertThat(rule.allowed("/schedule", "inplay/0.1")).isTrue();
        assertThat(rule.allowed("/private/a", "inplay/0.1")).isFalse();
        assertThat(rule.allowed("/schedule", "OtherBot/1.0")).isFalse();
    }

    @Test
    void commentsAndBlankLinesIgnored() {
        String body = """
                # comment
                User-agent: *
                # block private
                Disallow: /private

                """;
        RobotsTxtRule rule = RobotsTxtRule.parse(body);
        assertThat(rule.allowed("/private", "inplay/0.1")).isFalse();
        assertThat(rule.allowed("/", "inplay/0.1")).isTrue();
    }

    @Test
    void emptyDisallowMeansAllowAll() {
        String body = """
                User-agent: *
                Disallow:
                """;
        RobotsTxtRule rule = RobotsTxtRule.parse(body);
        assertThat(rule.allowed("/anything", "inplay/0.1")).isTrue();
    }

    @Test
    void allowAllStaticConstructor() {
        assertThat(RobotsTxtRule.allowAll().allowed("/", "x")).isTrue();
    }
}
