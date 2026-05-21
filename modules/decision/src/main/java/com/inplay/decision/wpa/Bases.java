package com.inplay.decision.wpa;

public record Bases(boolean first, boolean second, boolean third) {

    public static Bases empty() {
        return new Bases(false, false, false);
    }

    public static Bases of(int first, int second, int third) {
        return new Bases(first == 1, second == 1, third == 1);
    }

    public int index() {
        return (first ? 1 : 0) | (second ? 2 : 0) | (third ? 4 : 0);
    }

    public int runnersOn() {
        return (first ? 1 : 0) + (second ? 1 : 0) + (third ? 1 : 0);
    }

    public boolean loaded() {
        return first && second && third;
    }

    public boolean scoringPosition() {
        return second || third;
    }
}
