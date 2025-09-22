package com.axine.pokercasino.model.player;

public enum Сhip {
    ONE(1),
    FIVE(5),
    TEN(10),
    FIFTY(50),
    HUNDRED(100),
    FIVEHUNDRED(500),
    THOUSAND(1000);

    private final int value;

    Сhip(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
