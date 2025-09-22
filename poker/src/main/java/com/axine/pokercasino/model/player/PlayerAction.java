package com.axine.pokercasino.model.player;

public class PlayerAction {

    private final Event event;
    private final int amount;

    public PlayerAction(Event event, int amount) {
        this.event = event;
        this.amount = amount;
    }

    public Event getEvent() {
        return event;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return event + (amount > 0 ? " with bet " + amount : "");
    }
}