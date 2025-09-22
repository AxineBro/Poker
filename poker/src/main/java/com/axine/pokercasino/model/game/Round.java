package com.axine.pokercasino.model.game;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.player.Player;
import com.axine.pokercasino.model.player.PlayerAction;

import java.util.List;
import java.util.Map;

public interface Round {

    void startRound();

    void distributeCards();

    List<Card> dealCommunityCards(Stage stage);

    boolean checkRoundCompletion();

    void advanceToNextStage();

    Player evaluateHands(List<Player> players);

    List<Player> getWinners();

    void distributePot(List<Player> winners);

    void finishRound();

    void resetRound();

    List<Card> getCommunityCards();

    Stage getStage();

    int getPot();

    int getCurrentBet();

    List<Player> getPlayers();

    Map<Player, Integer> getPlayersBets();

    Player getCurrentPlayer();

    boolean isBettingComplete();

    void performBotAction();

    void submitHumanAction(PlayerAction action);

    int getDealerPos();

    void setDealerPos(int pos);
}