    package com.axine.pokercasino.model.deck.card;

    import com.fasterxml.jackson.annotation.JsonValue;

    public enum Rank {
        TWO(2, "2"),
        THREE(3, "3"),
        FOUR(4, "4"),
        FIVE(5, "5"),
        SIX(6, "6"),
        SEVEN(7, "7"),
        EIGHT(8, "8"),
        NINE(9, "9"),
        TEN(10, "10"),
        JACK(11, "J"),
        QUEEN(12, "Q"),
        KING(13, "K"),
        ACE(14, "A");

        private final int value;
        private final String shortName;

        Rank(int value, String shortName){
            this.value = value;
            this.shortName = shortName;
        }

        public int getValue(){
            return value;
        }

        @JsonValue
        public String getShortName() {
            return shortName;
        }
    }
