package com.inplay.api.dashboard;

import com.inplay.api.brief.DefaultUserProperties;
import com.inplay.core.domain.game.Game;
import com.inplay.decision.brief.BriefFormatter;
import com.inplay.decision.brief.BriefGenerator;
import com.inplay.ingest.game.GameMapper;
import com.inplay.ingest.game.GameRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final GameRepository repository;
    private final BriefGenerator briefGenerator;
    private final DefaultUserProperties user;
    private final Clock clock;

    public DashboardController(
            GameRepository repository,
            BriefGenerator briefGenerator,
            DefaultUserProperties user,
            Clock clock) {
        this.repository = repository;
        this.briefGenerator = briefGenerator;
        this.user = user;
        this.clock = clock;
    }

    @GetMapping("/")
    public String index(Model model) {
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("user", user);
        model.addAttribute("today", today);
        model.addAttribute("rows", todayRows(today));
        model.addAttribute("recent", recentRows(today));
        return "dashboard/index";
    }

    @GetMapping("/dashboard/today")
    public String todayFragment(Model model) {
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("user", user);
        model.addAttribute("today", today);
        model.addAttribute("rows", todayRows(today));
        return "dashboard/_today :: today";
    }

    private List<DashboardRow> todayRows(LocalDate date) {
        List<Game> seasonHistory = repository.findByDateBetween(seasonStart(date), date.minusDays(1)).stream()
                .map(GameMapper::toDomain)
                .toList();
        return repository.findByDateBetween(date, date).stream()
                .map(GameMapper::toDomain)
                .map(g -> {
                    var brief = briefGenerator.generate(g, seasonHistory, user.team());
                    return DashboardRow.of(g, BriefFormatter.format(brief), brief.isModelReady());
                })
                .toList();
    }

    private List<DashboardRow> recentRows(LocalDate today) {
        return repository.findByDateBetween(today.minusDays(7), today.minusDays(1)).stream()
                .map(GameMapper::toDomain)
                .map(g -> DashboardRow.of(g, null, false))
                .toList();
    }

    private static LocalDate seasonStart(LocalDate today) {
        return LocalDate.of(today.getYear(), Month.MARCH, 1);
    }
}
