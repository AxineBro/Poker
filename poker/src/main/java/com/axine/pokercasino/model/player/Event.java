package com.axine.pokercasino.model.player;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Event {
    FOLD("fold"),
    BET("bet"),
    CHECK("check");

    private final String shortName;

    Event(String shortName){
        this.shortName = shortName;
    }

    @JsonValue
    String getShortName(){
        return shortName;
    }
}