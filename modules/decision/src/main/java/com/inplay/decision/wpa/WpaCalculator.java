package com.inplay.decision.wpa;

// WPA — Win Probability Added.
// Convention: returned WPA is from the HOME team perspective.
// Caller can negate for the away team or remap to "batting team perspective" as needed.
//
// WPA(home) = WE_home(after) - WE_home(before)
//   - sum of WPA over all plays of a finished game ≈ ±1 - WE_home(initial) (away/home outcome)
//   - 0 < |WPA| ≤ 1.0
public final class WpaCalculator {

    public double calculate(GameState before, GameState after) {
        double weBefore = WinExpectancy.homeWinProb(before);
        double weAfter = WinExpectancy.homeWinProb(after);
        return weAfter - weBefore;
    }

    public double battingTeamWpa(GameState before, GameState after) {
        double homeWpa = calculate(before, after);
        return before.battingIsHome() ? homeWpa : -homeWpa;
    }
}
