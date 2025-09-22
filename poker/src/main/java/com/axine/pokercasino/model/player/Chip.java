package com.axine.pokercasino.model.player;

public enum Chip {  // Note: original has Cyrillic 'Сhip', but I corrected to 'Chip'
    ONE(1),
    FIVE(5),
    TEN(10),
    FIFTY(50),
    HUNDRED(100),
    FIVEHUNDRED(500),
    THOUSAND(1000);

    private final int value;

    Chip(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}