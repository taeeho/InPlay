package com.inplay.ingest.user;

import com.inplay.core.domain.id.UserId;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.core.domain.user.User;
import com.inplay.core.domain.user.UserMuteWindow;

import java.net.URI;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class UserMapper {

    private UserMapper() {}

    public static UserDocument toDocument(User user) {
        Objects.requireNonNull(user, "user required");
        Map<String, Double> weights = new LinkedHashMap<>();
        for (var e : user.rivalryWeights().entrySet()) {
            weights.put(e.getKey().code(), e.getValue());
        }
        UserMuteWindow mw = user.muteWindow();
        UserDocument.MuteWindowDoc muteDoc = new UserDocument.MuteWindowDoc(
                mw.start().toString(),
                mw.end().toString(),
                mw.zoneId().getId());
        return new UserDocument(
                null,
                user.userId().value(),
                user.name(),
                user.apiKeyHash(),
                user.myTeam().code(),
                weights,
                user.discordWebhookUrl() == null ? null : user.discordWebhookUrl().toString(),
                muteDoc,
                user.createdAt());
    }

    public static User toDomain(UserDocument doc) {
        Objects.requireNonNull(doc, "doc required");
        Map<KboTeam, Double> weights = new HashMap<>();
        if (doc.rivalryWeights() != null) {
            for (var e : doc.rivalryWeights().entrySet()) {
                weights.put(KboTeam.fromCode(e.getKey()), e.getValue());
            }
        }
        UserDocument.MuteWindowDoc mw = doc.muteWindow();
        UserMuteWindow muteWindow = mw == null
                ? UserMuteWindow.disabled()
                : new UserMuteWindow(LocalTime.parse(mw.start()), LocalTime.parse(mw.end()),
                        ZoneId.of(mw.timezone()));
        return new User(
                new UserId(doc.userId()),
                doc.name(),
                doc.apiKeyHash(),
                KboTeam.fromCode(doc.myTeam()),
                weights,
                doc.discordWebhookUrl() == null ? null : URI.create(doc.discordWebhookUrl()),
                muteWindow,
                doc.createdAt());
    }
}
