package com.axine.pokercasino.model.player.players;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.player.Event;
import com.axine.pokercasino.model.player.Player;
import com.axine.pokercasino.model.player.PlayerAction;

import java.util.ArrayList;
import java.util.List;

public class HumanPlayer implements Player {

    private final String name;
    private int chips;
    private List<Card> hand = new ArrayList<>();
    private boolean folded = false;

    public HumanPlayer(String name, int chips) {
        this.name = name;
        this.chips = chips;
    }

    @Override
    public void setChips(int value) {
        chips += value;
    }

    @Override
    public int getChips() {
        return chips;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Card> getHand() {
        return hand;
    }

    @Override
    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    @Override
    public PlayerAction event() {
        throw new UnsupportedOperationException(
                "HumanPlayer: event() вызывается только через интерфейс/контроллер!"
        );
    }

    @Override
    public boolean isFolded() {
        return folded;
    }

    @Override
    public void setFolded(boolean folded) {
        this.folded = folded;
    }
}