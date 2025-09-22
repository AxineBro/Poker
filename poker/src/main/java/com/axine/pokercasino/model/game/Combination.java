package com.axine.pokercasino.model.game;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Combination {
    ROYALFLUSH("royal flush", 9),
    STRAIGHTFLUSH("straight flush", 8),
    QUADS("quads", 7),
    FULLHOUSE("full house", 6),
    FLUSH("flush", 5),
    STRAIGHT("straight", 4),
    SET("set", 3),
    TWOPAIRS("two pairs", 2),
    ONEPAIR("one pair", 1),
    HIGHCARD("high card", 0);

    private final String name;
    private final int power;

    Combination(String name, int power){
        this.name = name;
        this.power = power;
    }

    public int getPower() {
        return power;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
