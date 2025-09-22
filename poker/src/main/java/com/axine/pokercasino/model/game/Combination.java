package com.axine.pokercasino.model.game;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Combination {
    HIGHCARD("high card", 0),
    ONEPAIR("one pair", 1),
    TWOPAIRS("two pairs", 2),
    SET("set", 3),
    STRAIGHT("straight", 4),
    FLUSH("flush", 5),
    FULLHOUSE("full house", 6),
    QUADS("quads", 7),
    STRAIGHTFLUSH("straight flush", 8),
    ROYALFLUSH("royal flush", 9);

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