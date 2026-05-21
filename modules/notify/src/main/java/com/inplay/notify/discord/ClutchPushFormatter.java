package com.inplay.notify.discord;

import com.inplay.core.domain.event.LiveEvent;
import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.InningHalf;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.decision.clutch.ClutchVerdict;
import com.inplay.decision.clutch.ImportanceScore;

import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Clutch push 메시지 포맷터.
 *
 * <p>PLAN.md §6 템플릿보다 단순화 — 베타에서는 투수 누적구수/ERA 같은 추가 컨텍스트가
 * 아직 없으므로 (LiveEvent + WPA + ImportanceScore) 만으로 구성.
 *
 * <p>예시:
 * <pre>
 * 🚨 **5회말 만루 1아웃**
 * 승률 0.55 → 0.78 (+0.23)
 * 중요도 7.5/10 · clutch 0.85
 * **한화** 4-3 LG
 * </pre>
 */
public final class ClutchPushFormatter {

    private ClutchPushFormatter() {}

    public static DiscordWebhookPayload format(
            LiveEvent previous,
            LiveEvent current,
            Game game,
            ClutchVerdict verdict,
            ImportanceScore importance) {
        Objects.requireNonNull(current, "current required");
        Objects.requireNonNull(game, "game required");
        Objects.requireNonNull(verdict, "verdict required");
        Objects.requireNonNull(importance, "importance required");

        return DiscordWebhookPayload.of(buildContent(previous, current, game, verdict, importance));
    }

    static String buildContent(LiveEvent previous, LiveEvent current, Game game,
                               ClutchVerdict verdict, ImportanceScore importance) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 **").append(headline(current)).append("**\n");

        double after = current.wpaAfter().orElse(Double.NaN);
        double before = previousWe(previous);
        if (!Double.isNaN(after)) {
            sb.append(String.format(Locale.ROOT, "승률 %.2f → %.2f (%+.2f)%n",
                    before, after, after - before));
        }

        Double clutchProb = verdict.probability();
        sb.append(String.format(Locale.ROOT, "중요도 %.1f/10", importance.value()));
        if (clutchProb != null) {
            sb.append(String.format(Locale.ROOT, " · clutch %.2f", clutchProb));
        }
        sb.append('\n');

        sb.append(scoreLine(game, current));
        return sb.toString();
    }

    private static double previousWe(LiveEvent prev) {
        if (prev == null) return 0.5;
        OptionalDouble v = prev.wpaAfter();
        return v.isPresent() ? v.getAsDouble() : 0.5;
    }

    private static String headline(LiveEvent e) {
        String half = e.half() == InningHalf.TOP ? "초" : "말";
        return e.inning() + "회" + half + " " + basesText(e) + " " + e.outs() + "아웃";
    }

    private static String basesText(LiveEvent e) {
        int runners = (e.runnerOn(1) ? 1 : 0) + (e.runnerOn(2) ? 1 : 0) + (e.runnerOn(3) ? 1 : 0);
        if (runners == 3) return "만루";
        if (runners == 0) return "주자없음";
        StringBuilder sb = new StringBuilder();
        if (e.runnerOn(1)) sb.append("1");
        if (e.runnerOn(2)) sb.append(sb.length() == 0 ? "2" : "·2");
        if (e.runnerOn(3)) sb.append(sb.length() == 0 ? "3" : "·3");
        sb.append("루");
        return sb.toString();
    }

    private static String scoreLine(Game game, LiveEvent current) {
        KboTeam home = game.homeTeam();
        KboTeam away = game.awayTeam();
        int homeRuns = current.score().home();
        int awayRuns = current.score().away();
        String homeMark = (homeRuns > awayRuns) ? "**" + home.code() + "**" : home.code();
        String awayMark = (awayRuns > homeRuns) ? "**" + away.code() + "**" : away.code();
        return homeMark + " " + homeRuns + "-" + awayRuns + " " + awayMark;
    }
}
