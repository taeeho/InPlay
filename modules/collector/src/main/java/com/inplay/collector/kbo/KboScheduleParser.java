package com.inplay.collector.kbo;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * KBO {@code /Schedule/Schedule.aspx} 의 SSR된 {@code #tblScheduleList} DOM 파싱.
 *
 * <p>실제 DOM 구조(2026-06 확인):
 * <ul>
 *   <li>row: {@code #tblScheduleList tbody tr}
 *   <li>날짜: {@code td.day} "MM.DD(요일)" — 같은 날 첫 경기에만 rowspan, 이후 행은 직전 날짜 carry.
 *       연도는 페이지 컨텍스트에 의존하므로 호출자가 인자로 전달(KBO 시즌은 단일 연도).
 *   <li>경기: {@code td.play} = {@code <span>원정</span><em>점수</em><span>홈</span>} (구장으로 검증: 오른쪽=홈)
 *   <li>점수: {@code em > span.win/.lose/.same} 숫자 2개 → FINAL, 점수 없이 vs만 → SCHEDULED
 *   <li>gameId: {@code td.relay a[href]} 의 gameId 파라미터(FINAL). 예정 경기는 href가 없어
 *       날짜+원정코드+홈코드+0 으로 합성.
 * </ul>
 *
 * <p>외부 한글 팀명을 내부 코드 키로 번역하는 경계 계층(anti-corruption layer) — 한글 팀명 매핑은
 * 여기서만 둔다(CLAUDE.md "팀명 한글 하드코딩 X"는 도메인/비즈니스 로직 대상, 외부 DOM 번역 경계는 예외).
 *
 * <p>현재 FINAL/SCHEDULED만 처리. LIVE/POSTPONED/CANCELED는 라이브 관찰 후 확장 예정.
 */
public final class KboScheduleParser {

    private record TeamRef(String gameIdCode, KboTeam team) {}

    private static final Map<String, TeamRef> TEAMS = Map.ofEntries(
            Map.entry("한화", new TeamRef("HH", KboTeam.HH)),
            Map.entry("두산", new TeamRef("OB", KboTeam.DOOSAN)),
            Map.entry("LG", new TeamRef("LG", KboTeam.LG)),
            Map.entry("KIA", new TeamRef("HT", KboTeam.KIA)),
            Map.entry("SSG", new TeamRef("SK", KboTeam.SSG)),
            Map.entry("KT", new TeamRef("KT", KboTeam.KT)),
            Map.entry("키움", new TeamRef("WO", KboTeam.KIWOOM)),
            Map.entry("NC", new TeamRef("NC", KboTeam.NC)),
            Map.entry("삼성", new TeamRef("SS", KboTeam.SAMSUNG)),
            Map.entry("롯데", new TeamRef("LT", KboTeam.LOTTE)));

    private static final Pattern DAY = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})");
    private static final Pattern GAME_ID = Pattern.compile("gameId=([0-9A-Za-z]+)");

    public List<Game> parse(String html, int year) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html);
        List<Game> games = new ArrayList<>();
        MonthDay carriedDay = null;
        for (Element row : doc.select("#tblScheduleList tbody tr")) {
            Element dayCell = row.selectFirst("td.day");
            if (dayCell != null) {
                Matcher m = DAY.matcher(dayCell.text());
                if (m.find()) {
                    carriedDay = MonthDay.of(
                            Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
                }
            }
            Element play = row.selectFirst("td.play");
            if (play == null || carriedDay == null) {
                continue;
            }
            Game game = parseRow(row, play, carriedDay, year);
            if (game != null) {
                games.add(game);
            }
        }
        return List.copyOf(games);
    }

    private Game parseRow(Element row, Element play, MonthDay md, int year) {
        List<Element> teamSpans = play.children().stream()
                .filter(e -> e.tagName().equals("span"))
                .toList();
        if (teamSpans.size() < 2) {
            return null;
        }
        TeamRef away = TEAMS.get(teamSpans.get(0).text().trim());
        TeamRef home = TEAMS.get(teamSpans.get(1).text().trim());
        if (away == null || home == null) {
            return null;
        }
        LocalDate date = LocalDate.of(year, md.getMonthValue(), md.getDayOfMonth());

        List<Element> scoreSpans =
                play.select("em > span.win, em > span.lose, em > span.same");
        GameStatus status;
        Score score;
        if (scoreSpans.size() >= 2) {
            int awayScore = parseScore(scoreSpans.get(0).text());
            int homeScore = parseScore(scoreSpans.get(1).text());
            score = new Score(homeScore, awayScore);
            status = GameStatus.FINAL;
        } else {
            score = Score.zero();
            status = GameStatus.SCHEDULED;
        }

        String gameId = extractGameId(row);
        if (gameId == null) {
            gameId = "%d%02d%02d%s%s0".formatted(
                    year, md.getMonthValue(), md.getDayOfMonth(),
                    away.gameIdCode(), home.gameIdCode());
        }
        return new Game(new GameId(gameId), date, home.team(), away.team(), status, score);
    }

    private static String extractGameId(Element row) {
        Element link = row.selectFirst("td.relay a[href]");
        if (link == null) {
            return null;
        }
        Matcher m = GAME_ID.matcher(link.attr("href"));
        return m.find() ? m.group(1) : null;
    }

    private static int parseScore(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
