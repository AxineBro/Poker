package com.axine.pokercasino.model.game.gameFactory;

import com.axine.pokercasino.model.deck.Deck;
import com.axine.pokercasino.model.game.Round;
import com.axine.pokercasino.model.game.RoundFactory;
import com.axine.pokercasino.model.game.games.OmahaHoldemRound;
import com.axine.pokercasino.model.player.Player;

import java.util.List;

public class OmahaHoldemRoundFactory extends RoundFactory {

    @Override
    public Round createRound(List<Player> players, Deck deck, int smallBlind, int bigBlind) {
        return new OmahaHoldemRound(players, deck, smallBlind, bigBlind);
    }
}