package com.axine.pokercasino.model.player.players;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.player.Event;
import com.axine.pokercasino.model.player.Player;
import com.axine.pokercasino.model.player.PlayerAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BotAIPlayer implements Player {

    private final String name;
    private int chips;
    private List<Card> hand = new ArrayList<>();
    private final Random random = new Random();
    private final int bigBlind = 20; // Default, ideally pass from GameService
    private boolean folded = false;

    public BotAIPlayer(String name, int chips) {
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
        int choice = random.nextInt(3);
        switch (choice) {
            case 0:
                return new PlayerAction(Event.FOLD, 0);
            case 1:
                return new PlayerAction(Event.CHECK, 0);
            case 2:
                int bet = Math.min(chips, Math.max(bigBlind, random.nextInt(500) + 1));
                return new PlayerAction(Event.BET, bet);
            default:
                return new PlayerAction(Event.CHECK, 0);
        }
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