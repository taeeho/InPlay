package com.inplay.collector.kbo;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 실제 KBO {@code #tblScheduleList} DOM 구조(2026-06 확인) 기반 fixture. */
class KboScheduleParserTest {

    private static final int YEAR = 2026;
    private final KboScheduleParser parser = new KboScheduleParser();

    @Test
    void parsesFinalGameWithWinLose() {
        String html = """
                <table id="tblScheduleList"><tbody>
                  <tr>
                    <td class="day" rowspan="1">06.02(화)</td>
                    <td class="time"><b>18:30</b></td>
                    <td class="play"><span>한화</span><em><span class="lose">3</span><span>vs</span><span class="win">5</span></em><span>두산</span></td>
                    <td class="relay"><a href="?gameId=20260602HHOB0">리뷰</a></td>
                    <td>잠실</td><td>-</td>
                  </tr>
                </tbody></table>
                """;
        List<Game> games = parser.parse(html, YEAR);
        assertThat(games).hasSize(1);
        Game g = games.get(0);
        assertThat(g.id().value()).isEqualTo("20260602HHOB0");
        assertThat(g.date()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(g.awayTeam()).isEqualTo(KboTeam.HH);
        assertThat(g.homeTeam()).isEqualTo(KboTeam.DOOSAN);
        assertThat(g.status()).isEqualTo(GameStatus.FINAL);
        assertThat(g.score().home()).isEqualTo(5);
        assertThat(g.score().away()).isEqualTo(3);
    }

    @Test
    void carriesDateAcrossRowspan() {
        String html = """
                <table id="tblScheduleList"><tbody>
                  <tr>
                    <td class="day" rowspan="2">06.02(화)</td>
                    <td class="play"><span>한화</span><em><span class="lose">3</span><span>vs</span><span class="win">5</span></em><span>두산</span></td>
                    <td class="relay"><a href="?gameId=20260602HHOB0">리뷰</a></td>
                  </tr>
                  <tr>
                    <td class="play"><span>NC</span><em><span class="lose">7</span><span>vs</span><span class="win">8</span></em><span>삼성</span></td>
                    <td class="relay"><a href="?gameId=20260602NCSS0">리뷰</a></td>
                  </tr>
                </tbody></table>
                """;
        List<Game> games = parser.parse(html, YEAR);
        assertThat(games).hasSize(2);
        // 2번째 행은 td.day 없음 → 직전 날짜 carry
        assertThat(games.get(1).date()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(games.get(1).awayTeam()).isEqualTo(KboTeam.NC);
        assertThat(games.get(1).homeTeam()).isEqualTo(KboTeam.SAMSUNG);
    }

    @Test
    void parsesDrawWithSameClass() {
        String html = """
                <table id="tblScheduleList"><tbody>
                  <tr>
                    <td class="day">06.03(수)</td>
                    <td class="play"><span>한화</span><em><span class="same">3</span><span>vs</span><span class="same">3</span></em><span>두산</span></td>
                    <td class="relay"><a href="?gameId=20260603HHOB0">리뷰</a></td>
                  </tr>
                </tbody></table>
                """;
        List<Game> games = parser.parse(html, YEAR);
        assertThat(games).hasSize(1);
        assertThat(games.get(0).status()).isEqualTo(GameStatus.FINAL);
        assertThat(games.get(0).score().home()).isEqualTo(3);
        assertThat(games.get(0).score().away()).isEqualTo(3);
    }

    @Test
    void parsesScheduledGameSynthesizingGameId() {
        // 예정 경기: 점수 없이 vs만, td.relay 비어 href 없음 → 날짜+원정코드+홈코드+0 합성
        String html = """
                <table id="tblScheduleList"><tbody>
                  <tr>
                    <td class="day">06.05(금)</td>
                    <td class="time"><b>18:30</b></td>
                    <td class="play"><span>삼성</span><em><span>vs</span></em><span>NC</span></td>
                    <td class="relay"></td>
                    <td>창원</td><td>-</td>
                  </tr>
                </tbody></table>
                """;
        List<Game> games = parser.parse(html, YEAR);
        assertThat(games).hasSize(1);
        Game g = games.get(0);
        assertThat(g.status()).isEqualTo(GameStatus.SCHEDULED);
        assertThat(g.score().home()).isZero();
        assertThat(g.score().away()).isZero();
        assertThat(g.awayTeam()).isEqualTo(KboTeam.SAMSUNG);
        assertThat(g.homeTeam()).isEqualTo(KboTeam.NC);
        assertThat(g.id().value()).isEqualTo("20260605SSNC0");
    }

    @Test
    void emptyOrNullHtmlReturnsEmpty() {
        assertThat(parser.parse("", YEAR)).isEmpty();
        assertThat(parser.parse(null, YEAR)).isEmpty();
    }

    @Test
    void skipsRowWithUnknownTeam() {
        String html = """
                <table id="tblScheduleList"><tbody>
                  <tr>
                    <td class="day">06.02(화)</td>
                    <td class="play"><span>한화</span><em><span class="win">5</span><span>vs</span><span class="lose">3</span></em><span>알수없음</span></td>
                    <td class="relay"><a href="?gameId=20260602HHXX0">리뷰</a></td>
                  </tr>
                </tbody></table>
                """;
        assertThat(parser.parse(html, YEAR)).isEmpty();
    }
}
