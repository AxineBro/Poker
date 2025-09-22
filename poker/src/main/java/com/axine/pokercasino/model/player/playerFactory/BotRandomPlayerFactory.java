package com.axine.pokercasino.model.player.playerFactory;

import com.axine.pokercasino.model.player.Player;
import com.axine.pokercasino.model.player.PlayerFactory;
import com.axine.pokercasino.model.player.players.BotRandomPlayer;

public class BotRandomPlayerFactory extends PlayerFactory {

    @Override
    public Player createPlayer(String name, int chips){
        return new BotRandomPlayer(name, chips);
    }
}