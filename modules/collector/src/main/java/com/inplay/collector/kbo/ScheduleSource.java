package com.inplay.collector.kbo;

import com.inplay.core.domain.game.Game;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleSource {
    List<Game> fetchSchedule(LocalDate from, LocalDate to);
}
