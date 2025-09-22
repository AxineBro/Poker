package com.axine.pokercasino.model.deck;

import java.util.List;

public interface Deck {

    void initialize();

    void shuffle();

    Card getCard();

    List<Card> getCards(int num);

    int getCardsCount();
}