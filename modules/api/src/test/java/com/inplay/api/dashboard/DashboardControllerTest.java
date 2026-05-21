package com.inplay.api.dashboard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inplay.api.brief.DefaultUserProperties;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.decision.brief.BriefGenerator;
import com.inplay.decision.brief.WinProbabilityBrief;
import com.inplay.ingest.game.GameDocument;
import com.inplay.ingest.game.GameRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DashboardControllerTest.TestConfig.class)
class DashboardControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 20);

    @Autowired MockMvc mvc;
    @MockBean GameRepository repository;
    @MockBean BriefGenerator briefGenerator;

    static class TestConfig {
        @Bean DefaultUserProperties user() {
            return new DefaultUserProperties("taeeho", KboTeam.HH, "Asia/Seoul", null, null, null);
        }
        @Bean Clock clock() {
            return Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        }
    }

    @Test
    void indexReturns200AndShowsUser() throws Exception {
        when(repository.findByDateBetween(any(), any())).thenReturn(List.of());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("user", "today", "rows", "recent"))
                .andExpect(content().string(Matchers.containsString("taeeho")))
                .andExpect(content().string(Matchers.containsString("KBO 동반시청")));
    }

    @Test
    void todayFragmentReturnsPartialMarkup() throws Exception {
        when(repository.findByDateBetween(any(), any())).thenReturn(List.of());

        mvc.perform(get("/dashboard/today"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("today-block")))
                .andExpect(content().string(Matchers.containsString("새로고침")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("<!doctype html>"))));
    }

    @Test
    void indexRendersTodayGames() throws Exception {
        var todayGame = GameDocument.forNew(
                "20260520HHLG", TODAY, "HH", "LG", "SCHEDULED",
                new GameDocument.ScoreDocument(0, 0));
        when(repository.findByDateBetween(TODAY, TODAY)).thenReturn(List.of(todayGame));
        when(repository.findByDateBetween(LocalDate.of(2026, 3, 1), TODAY.minusDays(1))).thenReturn(List.of());
        when(repository.findByDateBetween(TODAY.minusDays(7), TODAY.minusDays(1))).thenReturn(List.of());
        when(briefGenerator.generate(any(), any(), any()))
                .thenReturn(new WinProbabilityBrief(
                        new com.inplay.core.domain.id.GameId("20260520HHLG"),
                        TODAY, KboTeam.HH, KboTeam.LG,
                        0.62, KboTeam.HH, null));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("HH")))
                .andExpect(content().string(Matchers.containsString("LG")))
                .andExpect(content().string(Matchers.containsString("62.0%")));
    }
}
