package com.axine.pokercasino.model.player;

import com.axine.pokercasino.model.deck.Card;

import java.util.List;

public abstract class PlayerFactory {

    public abstract Player createPlayer(String name, int chips);

    public int getChips(Player player) {
        return player.getChips();
    }

    public void setChips(Player player, int value) {
        player.setChips(value);
    }

    public String getName(Player player) {
        return player.getName();
    }

    public List<Card> getHand(Player player) {
        return player.getHand();
    }

    public void setHand(Player player, List<Card> hand) {
        player.setHand(hand);
    }

    public PlayerAction event(Player player) {
        return player.event();
    }
}
