package com.inplay.ingest.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.id.UserId;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.core.domain.user.User;
import com.inplay.core.domain.user.UserMuteWindow;

import java.net.URI;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private static final String HASH = "a".repeat(64);

    private User rich() {
        return new User(
                new UserId("u_taeeho"),
                "taeeho",
                HASH,
                KboTeam.HH,
                Map.of(KboTeam.LG, 1.3, KboTeam.KIA, 1.25),
                URI.create("https://discord.com/api/webhooks/x/y"),
                new UserMuteWindow(LocalTime.of(8, 0), LocalTime.of(10, 0), ZoneId.of("Asia/Seoul")),
                Instant.parse("2026-05-12T00:00:00Z"));
    }

    private User sparse() {
        return new User(
                new UserId("u_solo"),
                "solo",
                "b".repeat(64),
                KboTeam.LG,
                Map.of(),
                null,
                UserMuteWindow.disabled(),
                Instant.parse("2026-05-12T00:00:00Z"));
    }

    @Test
    void richUserRoundTrips() {
        var doc = UserMapper.toDocument(rich());
        var back = UserMapper.toDomain(doc);
        assertThat(back.userId().value()).isEqualTo("u_taeeho");
        assertThat(back.myTeam()).isEqualTo(KboTeam.HH);
        assertThat(back.weightFor(KboTeam.LG)).isEqualTo(1.3);
        assertThat(back.discordWebhookUrl().toString()).isEqualTo("https://discord.com/api/webhooks/x/y");
        assertThat(back.muteWindow().start()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void sparseUserOmitsOptionalFields() {
        var doc = UserMapper.toDocument(sparse());
        assertThat(doc.discordWebhookUrl()).isNull();
        var back = UserMapper.toDomain(doc);
        assertThat(back.discordWebhookUrl()).isNull();
        assertThat(back.muteWindow().isDisabled()).isTrue();
    }

    @Test
    void rivalryWeightsMapsTeamCodes() {
        var doc = UserMapper.toDocument(rich());
        assertThat(doc.rivalryWeights()).containsKeys("LG", "KIA");
    }

    @Test
    void apiKeyHashStaysVerbatim() {
        var doc = UserMapper.toDocument(rich());
        assertThat(doc.apiKeyHash()).isEqualTo(HASH);
    }
}
