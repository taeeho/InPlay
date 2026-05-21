package com.inplay.core.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.inplay.core.domain.id.UserId;
import com.inplay.core.domain.team.KboTeam;

import java.net.URI;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final String VALID_HASH = "a".repeat(64);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private User sample() {
        return new User(
                new UserId("u_taeeho"),
                "taeeho",
                VALID_HASH,
                KboTeam.HH,
                Map.of(KboTeam.LG, 1.3, KboTeam.KIA, 1.25),
                URI.create("https://discord.com/api/webhooks/x/y"),
                new UserMuteWindow(LocalTime.of(8, 0), LocalTime.of(10, 0), KST),
                Instant.parse("2026-05-12T00:00:00Z"));
    }

    @Test
    void buildsHappyPath() {
        var u = sample();
        assertThat(u.name()).isEqualTo("taeeho");
        assertThat(u.weightFor(KboTeam.LG)).isEqualTo(1.3);
        assertThat(u.weightFor(KboTeam.SSG)).isEqualTo(1.0);
    }

    @Test
    void rivalryWeightsIsImmutableCopy() {
        var weights = new java.util.HashMap<KboTeam, Double>();
        weights.put(KboTeam.LG, 1.3);
        var u = new User(
                new UserId("u"), "n", VALID_HASH, KboTeam.HH, weights,
                null, UserMuteWindow.disabled(), Instant.now());
        weights.put(KboTeam.KIA, 99.0);
        assertThat(u.weightFor(KboTeam.KIA)).isEqualTo(1.0);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new User(
                new UserId("u"), "  ", VALID_HASH, KboTeam.HH, Map.of(),
                null, UserMuteWindow.disabled(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsHashOfWrongLength() {
        assertThatThrownBy(() -> new User(
                new UserId("u"), "n", "abc", KboTeam.HH, Map.of(),
                null, UserMuteWindow.disabled(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sha256 hex");
    }

    @Test
    void rejectsNonHexHash() {
        String badHash = "g".repeat(64);
        assertThatThrownBy(() -> new User(
                new UserId("u"), "n", badHash, KboTeam.HH, Map.of(),
                null, UserMuteWindow.disabled(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void discordWebhookCanBeNull() {
        var u = new User(
                new UserId("u"), "n", VALID_HASH, KboTeam.HH, Map.of(),
                null, UserMuteWindow.disabled(), Instant.now());
        assertThat(u.discordWebhookUrl()).isNull();
    }
}
