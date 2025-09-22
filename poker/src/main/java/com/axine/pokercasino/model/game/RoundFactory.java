package com.axine.pokercasino.model.game;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.deck.Deck;
import com.axine.pokercasino.model.player.Player;
import com.axine.pokercasino.model.player.PlayerAction;
import com.axine.pokercasino.model.player.players.HumanPlayer;

import java.util.List;
import java.util.Map;

public abstract class RoundFactory {

    public abstract Round createRound(List<Player> players, Deck deck, int smallBlind, int bigBlind);

    public void startRound(Round round) {
        if (round == null) throw new IllegalArgumentException("Round cannot be null");
        round.startRound();
    }

    public void distributeCards(Round round) {
        if (round == null) throw new IllegalArgumentException("Round cannot be null");
        round.distributeCards();
    }

    public List<Card> dealCommunityCards(Stage stage, Round round) {
        if (round == null || stage == null) throw new IllegalArgumentException("Round or stage cannot be null");
        return round.dealCommunityCards(stage);
    }

    public boolean isBettingComplete(Round round) {
        if (round == null) return true;
        return round.isBettingComplete();
    }

    public void performBotAction(Round round) {
        if (round == null) throw new IllegalArgumentException("Round cannot be null");
        round.performBotAction();
    }

    public void submitHumanAction(Round round, PlayerAction action) {
        if (round == null || action == null) throw new IllegalArgumentException("Round or action cannot be null");
        round.submitHumanAction(action);
    }

    public void manageBettingRound(Round round, PlayerAction humanAction) {
        if (round == null) return;
        int maxAttempts = round.getPlayers().size() * 10; // Increased to handle raises
        int attempts = 0;
        while (!round.isBettingComplete() && attempts < maxAttempts) {
            Player currentPlayer = round.getCurrentPlayer();
            if (currentPlayer == null) {
                break;
            }
            if (currentPlayer instanceof HumanPlayer) {
                if (humanAction != null) {
                    round.submitHumanAction(humanAction);
                    humanAction = null;
                } else {
                    break; // Wait for human
                }
            } else {
                round.performBotAction();
            }
            attempts++;
        }
        if (attempts >= maxAttempts) {
            throw new IllegalStateException("Betting round exceeded max attempts – possible infinite loop");
        }
    }

    public boolean checkRoundCompletion(Round round) {
        if (round == null) return true;
        return round.checkRoundCompletion();
    }

    public void advanceToNextStage(Round round) {
        if (round == null) throw new IllegalArgumentException("Round cannot be null");
        round.advanceToNextStage();
    }

    public Player evaluateHands(List<Player> players, Round round) {
        if (round == null) return null;
        return round.evaluateHands(players);
    }

    public List<Player> getWinners(Round round) {
        if (round == null) return List.of();
        return round.getWinners();
    }

    public void distributePot(List<Player> winners, Round round) {
        if (round == null || winners == null) return;
        round.distributePot(winners);
    }

    public void finishRound(Round round) {
        if (round == null) return;
        round.finishRound();
    }

    public void resetRound(Round round) {
        if (round == null) return;
        round.resetRound();
    }

    public List<Card> getCommunityCards(Round round) {
        if (round == null) return List.of();
        return round.getCommunityCards();
    }

    public Stage getStage(Round round) {
        if (round == null) return Stage.PREFLOP;
        return round.getStage();
    }

    public int getPot(Round round) {
        if (round == null) return 0;
        return round.getPot();
    }

    public int getCurrentBet(Round round) {
        if (round == null) return 0;
        return round.getCurrentBet();
    }

    public List<Player> getPlayers(Round round) {
        if (round == null) return List.of();
        return round.getPlayers();
    }

    public Map<Player, Integer> getPlayersBets(Round round) {
        if (round == null) return Map.of();
        return round.getPlayersBets();
    }

    public Player getCurrentPlayer(Round round) {
        if (round == null) return null;
        return round.getCurrentPlayer();
    }
}