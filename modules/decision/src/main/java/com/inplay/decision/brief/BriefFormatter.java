package com.inplay.decision.brief;

import com.inplay.core.domain.team.KboTeam;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * WinProbabilityBrief → Discord 메시지 텍스트. 단순 markdown.
 */
public final class BriefFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN);

    private BriefFormatter() {}

    public static String format(WinProbabilityBrief brief) {
        Objects.requireNonNull(brief, "brief required");

        StringBuilder sb = new StringBuilder();
        sb.append("**").append(DATE.format(brief.date())).append(" KBO**\n");
        sb.append(markTeam(brief.awayTeam(), brief.highlightTeam()))
                .append(" @ ")
                .append(markTeam(brief.homeTeam(), brief.highlightTeam()))
                .append('\n');

        if (brief.isModelReady()) {
            double home = brief.homeWinProbability();
            double away = 1.0 - home;
            sb.append(String.format(Locale.ROOT, "승률: 홈 %.1f%% / 원정 %.1f%%", home * 100, away * 100));
        } else {
            sb.append("승률: — (").append(brief.note() == null ? "모델 미준비" : brief.note()).append(")");
        }
        return sb.toString();
    }

    private static String markTeam(KboTeam team, KboTeam highlight) {
        String code = team.code();
        return team == highlight ? "**" + code + "**" : code;
    }
}
