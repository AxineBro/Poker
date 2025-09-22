package com.axine.pokercasino.model.deck.deckFactory;

import com.axine.pokercasino.model.deck.Deck;
import com.axine.pokercasino.model.deck.DeckFactory;
import com.axine.pokercasino.model.deck.decks.StandardDeck;

public class StandardDeckFactory extends DeckFactory {

    @Override
    public Deck createDeck() {
        return new StandardDeck();
    }
}