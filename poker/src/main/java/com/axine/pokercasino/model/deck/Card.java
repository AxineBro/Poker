package com.axine.pokercasino.model.deck;

import com.axine.pokercasino.model.deck.card.Rank;
import com.axine.pokercasino.model.deck.card.Suit;

public class Card {
    private Suit suit;
    private Rank rank;

    public Card(Suit suit, Rank rank){
        this.suit = suit;
        this.rank = rank;
    }

    public Rank getRank() {
        return rank;
    }

    public Suit getSuit(){
        return suit;
    }
}