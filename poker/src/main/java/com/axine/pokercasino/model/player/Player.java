package com.axine.pokercasino.model.player;

import com.axine.pokercasino.model.deck.Card;

import java.util.List;

public interface Player {

    int getChips();

    void setChips(int value);

    String getName();

    List<Card> getHand();

    void setHand(List<Card> hand);

    PlayerAction event();

    boolean isFolded();

    void setFolded(boolean folded);
}