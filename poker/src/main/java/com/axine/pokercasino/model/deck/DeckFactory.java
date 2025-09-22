package com.axine.pokercasino.model.deck;

import java.util.List;

public abstract class DeckFactory {

    public abstract Deck createDeck();

    public void shuffleDeck(Deck deck) {
        if (deck == null) throw new IllegalArgumentException("Deck cannot be null");
        deck.shuffle();
    }

    public Card getCard(Deck deck) {
        if (deck == null) throw new IllegalArgumentException("Deck cannot be null");
        return deck.getCard();
    }

    public List<Card> getCards(Deck deck, int num) {
        if (deck == null) throw new IllegalArgumentException("Deck cannot be null");
        if (num < 0) throw new IllegalArgumentException("Number of cards cannot be negative");
        return deck.getCards(num);
    }

    public int getCardsCount(Deck deck) {
        if (deck == null) return 0;
        return deck.getCardsCount();
    }
}