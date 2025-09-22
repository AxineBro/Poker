package com.axine.pokercasino.model.deck.card;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Suit {
    HEARTS("H"),
    DIAMONDS("D"),
    CLUBS("C"),
    SPADES("S");

    private final String shortName;

    Suit(String shortName){
        this.shortName = shortName;
    }

    @JsonValue
    public String getShortName() {
        return shortName;
    }
}
