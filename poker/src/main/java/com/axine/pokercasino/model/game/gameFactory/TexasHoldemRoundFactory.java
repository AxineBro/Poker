package com.axine.pokercasino.model.game.gameFactory;

import com.axine.pokercasino.model.deck.Deck;
import com.axine.pokercasino.model.game.Round;
import com.axine.pokercasino.model.game.RoundFactory;
import com.axine.pokercasino.model.game.games.TexasHoldemRound;
import com.axine.pokercasino.model.player.Player;

import java.util.List;

public class TexasHoldemRoundFactory extends RoundFactory {

    @Override
    public Round createRound(List<Player> players, Deck deck, int smallBlind, int bigBlind) {
        return new TexasHoldemRound(players, deck, smallBlind, bigBlind);
    }
}