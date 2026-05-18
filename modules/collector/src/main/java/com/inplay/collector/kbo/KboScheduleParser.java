package com.inplay.collector.kbo;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class KboScheduleParser {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public List<Game> parse(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html);
        List<Game> games = new ArrayList<>();
        for (Element row : doc.select("table.schedule tr.game")) {
            Game game = parseRow(row);
            if (game != null) {
                games.add(game);
            }
        }
        return List.copyOf(games);
    }

    private Game parseRow(Element row) {
        String gameId = row.attr("data-game-id");
        String date = row.attr("data-date");
        String home = textOf(row, "td.home");
        String away = textOf(row, "td.away");
        String status = textOf(row, "td.status");
        String score = textOf(row, "td.score");
        if (gameId.isEmpty() || date.isEmpty() || home.isEmpty() || away.isEmpty() || status.isEmpty()) {
            return null;
        }
        return new Game(
                new GameId(gameId),
                LocalDate.parse(date, DATE),
                KboTeam.fromCode(home),
                KboTeam.fromCode(away),
                GameStatus.valueOf(status),
                parseScore(score));
    }

    private static String textOf(Element row, String selector) {
        Element el = row.selectFirst(selector);
        return el == null ? "" : el.text().trim();
    }

    private static Score parseScore(String text) {
        if (text.isEmpty() || !text.contains("-")) {
            return Score.zero();
        }
        String[] parts = text.split("-", 2);
        try {
            int home = Integer.parseInt(parts[0].trim());
            int away = Integer.parseInt(parts[1].trim());
            return new Score(home, away);
        } catch (NumberFormatException ex) {
            return Score.zero();
        }
    }
}
