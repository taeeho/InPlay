package com.inplay.core.domain.team;

import java.util.Objects;

public enum KboTeam {
    HH,
    LG,
    KIA,
    SSG,
    KT,
    KIWOOM,
    NC,
    SAMSUNG,
    LOTTE,
    DOOSAN;

    public String code() {
        return name();
    }

    public static KboTeam fromCode(String code) {
        Objects.requireNonNull(code, "code required");
        for (KboTeam team : values()) {
            if (team.name().equals(code)) {
                return team;
            }
        }
        throw new IllegalArgumentException("unknown KBO team code: " + code);
    }
}
