package com.axine.pokercasino.model.deck.decks;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.deck.Deck;
import com.axine.pokercasino.model.deck.card.Rank;
import com.axine.pokercasino.model.deck.card.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StandardDeck implements Deck {
    private final List<Card> cards;
    private int currentIndex;

    public StandardDeck() {
        cards = new ArrayList<>();
        currentIndex = 0;
        initialize();
    }

    @Override
    public void initialize() {
        cards.clear();
        currentIndex = 0;
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    @Override
    public void shuffle() {
        Collections.shuffle(cards);
        currentIndex = 0;
    }

    @Override
    public Card getCard() {
        if (currentIndex >= cards.size()) {
            throw new IllegalStateException("Not enough cards in deck");
        }
        return cards.get(currentIndex++);
    }

    @Override
    public List<Card> getCards(int num) {
        if (num < 0) throw new IllegalArgumentException("Number of cards cannot be negative");
        if (currentIndex + num > cards.size()) {
            throw new IllegalStateException("Not enough cards in deck: requested " + num + ", available " + (cards.size() - currentIndex));
        }
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            result.add(cards.get(currentIndex++));
        }
        return result;
    }

    @Override
    public int getCardsCount() {
        return cards.size() - currentIndex;
    }
}