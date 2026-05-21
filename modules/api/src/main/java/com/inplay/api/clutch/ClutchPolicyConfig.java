package com.inplay.api.clutch;

import com.inplay.api.brief.DefaultUserProperties;
import com.inplay.core.domain.team.KboTeam;
import com.inplay.decision.clutch.ClutchNotifyPolicy;
import com.inplay.decision.clutch.ImportanceScorer;
import com.inplay.decision.clutch.MuteWindow;
import com.inplay.decision.clutch.RivalrySettings;
import com.inplay.decision.wpa.WpaAnnotator;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClutchProperties.class)
public class ClutchPolicyConfig {

    @Bean
    public RivalrySettings rivalrySettings(DefaultUserProperties user) {
        Map<KboTeam, Double> weights = user.rivalryWeights() == null ? Map.of() : user.rivalryWeights();
        return new RivalrySettings(user.team(), weights);
    }

    @Bean
    public ImportanceScorer importanceScorer() {
        return new ImportanceScorer();
    }

    @Bean
    public MuteWindow clutchMuteWindow(DefaultUserProperties user) {
        var win = user.muteWindow();
        if (win == null || win.start() == null || win.end() == null) {
            return MuteWindow.none();
        }
        ZoneId zone = user.timezone() == null ? ZoneId.of("Asia/Seoul") : ZoneId.of(user.timezone());
        return new MuteWindow(LocalTime.parse(win.start()), LocalTime.parse(win.end()), zone);
    }

    @Bean
    public WpaAnnotator wpaAnnotator() {
        return new WpaAnnotator();
    }

    @Bean
    public ClutchNotifyPolicy clutchNotifyPolicy(ClutchProperties props,
                                                 MuteWindow muteWindow,
                                                 Clock briefClock) {
        return new ClutchNotifyPolicy(
                props.importanceThreshold(),
                props.cooldown(),
                props.dedupeTtl(),
                muteWindow,
                briefClock);
    }
}
