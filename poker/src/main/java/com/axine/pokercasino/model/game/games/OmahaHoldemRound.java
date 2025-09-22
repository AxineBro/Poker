package com.axine.pokercasino.model.game.games;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.deck.Deck;
import com.axine.pokercasino.model.game.Combination;
import com.axine.pokercasino.model.game.EvaluationCombination;
import com.axine.pokercasino.model.game.Round;
import com.axine.pokercasino.model.game.Stage;
import com.axine.pokercasino.model.player.Event;
import com.axine.pokercasino.model.player.Player;
import com.axine.pokercasino.model.player.PlayerAction;
import com.axine.pokercasino.model.player.players.HumanPlayer;

import java.util.*;

public class OmahaHoldemRound implements Round {
    private List<Player> players;
    private List<Card> desk;
    private Deck deck;
    private int sharedBank;
    private int currentBet;
    private Map<Player, Integer> playersBet;
    private Stage stage;
    private int dealerPos;
    private int currentPlayerIndex;
    private final int smallBlind;
    private final int bigBlind;
    private Set<Player> actedThisRound;

    public OmahaHoldemRound(List<Player> players, Deck deck, int smallBlind, int bigBlind) {
        if (players == null || deck == null) throw new IllegalArgumentException("Players or deck cannot be null");
        if (players.size() < 2) throw new IllegalArgumentException("At least 2 players required");
        this.players = new ArrayList<>(players);
        this.deck = deck;
        this.desk = new ArrayList<>();
        this.playersBet = new HashMap<>();
        this.dealerPos = 0;
        this.currentPlayerIndex = 0;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.actedThisRound = new HashSet<>();
    }

    @Override
    public void startRound() {
        if (deck.getCardsCount() < players.size() * 4 + 5) {
            throw new IllegalStateException("Not enough cards in deck");
        }
        deck.shuffle();
        sharedBank = 0;
        currentBet = 0;
        playersBet = new HashMap<>();
        desk = new ArrayList<>();
        stage = Stage.PREFLOP;
        actedThisRound = new HashSet<>();

        for (Player p : players) {
            p.setFolded(false);
        }

        int smallPos = (dealerPos + 1) % players.size();
        int bigPos = (dealerPos + 2) % players.size();
        Player smallPlayer = players.get(smallPos);
        Player bigPlayer = players.get(bigPos);

        int smallToPay = Math.min(smallBlind, smallPlayer.getChips());
        smallPlayer.setChips(-smallToPay);
        sharedBank += smallToPay;
        playersBet.put(smallPlayer, smallToPay);
        actedThisRound.add(smallPlayer);
        currentBet = smallToPay;

        int bigToPay = Math.min(bigBlind, bigPlayer.getChips());
        bigPlayer.setChips(-bigToPay);
        sharedBank += bigToPay;
        playersBet.put(bigPlayer, bigToPay);
        actedThisRound.add(bigPlayer);
        currentBet = bigToPay;

        currentPlayerIndex = (dealerPos + 3) % players.size();
    }

    @Override
    public void distributeCards() {
        if (deck.getCardsCount() < players.size() * 4) {
            throw new IllegalStateException("Not enough cards for dealing");
        }
        for (Player player : players) {
            player.setHand(deck.getCards(4));
        }
    }

    @Override
    public List<Card> dealCommunityCards(Stage stage) {
        if (deck.getCardsCount() < 1) {
            throw new IllegalStateException("Not enough cards to burn");
        }
        deck.getCard(); // Burn one card
        int numCards;
        switch (stage) {
            case FLOP:
                numCards = 3;
                break;
            case TURN:
            case RIVER:
                numCards = 1;
                break;
            default:
                throw new IllegalArgumentException("Invalid stage for dealing community cards");
        }
        if (deck.getCardsCount() < numCards) {
            throw new IllegalStateException("Not enough cards in deck for community cards");
        }
        List<Card> cards = deck.getCards(numCards);
        desk.addAll(cards);
        return cards;
    }

    @Override
    public boolean isBettingComplete() {
        List<Player> active = getActivePlayers();
        if (active.size() <= 1) return true;
        boolean allMatched = true;
        for (Player p : active) {
            int already = playersBet.getOrDefault(p, 0);
            if (already < currentBet) {
                allMatched = false;
                break;
            }
        }
        boolean allActed = actedThisRound.containsAll(active);
        return allMatched && allActed;
    }

    @Override
    public void performBotAction() {
        try {
            Thread.sleep(500); // Delay for smooth
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Player p = getCurrentPlayer();
        if (p == null) throw new IllegalStateException("No current player");
        if (p instanceof HumanPlayer) throw new IllegalStateException("Cannot perform bot action on human player");
        PlayerAction action = p.event();
        processAction(action, p);
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    @Override
    public void submitHumanAction(PlayerAction action) {
        Player p = getCurrentPlayer();
        if (p == null) throw new IllegalStateException("No current player");
        if (!(p instanceof HumanPlayer)) throw new IllegalStateException("Not human player's turn");
        processAction(action, p);
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    private void processAction(PlayerAction playerAction, Player player) {
        if (playerAction == null || player == null) throw new IllegalArgumentException("Action or player cannot be null");
        int balance = player.getChips();
        int alreadyBet = playersBet.getOrDefault(player, 0);
        int toPay;

        switch (playerAction.getEvent()) {
            case FOLD:
                player.setFolded(true);
                break;
            case CHECK:
                toPay = currentBet - alreadyBet;
                if (toPay > balance) {
                    toPay = balance; // All-in
                }
                sharedBank += toPay;
                player.setChips(-toPay);
                playersBet.put(player, alreadyBet + toPay);
                actedThisRound.add(player);
                break;
            case BET:
                int proposedTotal = playerAction.getAmount();
                if (proposedTotal <= currentBet) throw new IllegalArgumentException("Bet must be higher than current bet");
                toPay = proposedTotal - alreadyBet;
                if (toPay > balance) {
                    toPay = balance; // All-in
                    proposedTotal = alreadyBet + toPay;
                }
                sharedBank += toPay;
                player.setChips(-toPay);
                playersBet.put(player, proposedTotal);
                currentBet = proposedTotal;
                actedThisRound.clear();
                actedThisRound.add(player);
                break;
            default:
                throw new IllegalArgumentException("Invalid action: " + playerAction.getEvent());
        }
    }

    @Override
    public boolean checkRoundCompletion() {
        return stage == Stage.SHOWDOWN || getActivePlayers().size() <= 1;
    }

    @Override
    public void advanceToNextStage() {
        currentBet = 0;
        playersBet.clear();
        actedThisRound.clear();
        switch (stage) {
            case PREFLOP:
                stage = Stage.FLOP;
                break;
            case FLOP:
                stage = Stage.TURN;
                break;
            case TURN:
                stage = Stage.RIVER;
                break;
            case RIVER:
                stage = Stage.SHOWDOWN;
                break;
            default:
        }
        if (stage != Stage.SHOWDOWN) {
            currentPlayerIndex = (dealerPos + 1) % players.size();
        }
    }

    @Override
    public Player evaluateHands(List<Player> players) {
        List<Player> winners = getWinners();
        return winners.isEmpty() ? null : winners.get(0);
    }

    @Override
    public List<Player> getWinners() {
        if (!checkRoundCompletion()) return List.of();
        List<Player> active = getActivePlayers();
        if (active.size() <= 1) return active;
        return determineWinners(active);
    }

    private List<Player> getActivePlayers() {
        List<Player> active = new ArrayList<>();
        for (Player p : players) {
            if (!p.isFolded()) active.add(p);
        }
        return active;
    }

    private List<Player> determineWinners(List<Player> active) {
        List<Player> winners = new ArrayList<>();
        int maxValue = -1;

        for (Player player : active) {
            if (player.getHand() == null || player.getHand().size() != 4) {
                continue;
            }

            List<List<Card>> holeCombinations = generateCombinations(player.getHand(), 2);
            int bestComboValue = -1;

            for (List<Card> hole : holeCombinations) {
                List<List<Card>> communityCombinations = generateCombinations(desk, 3);
                for (List<Card> community : communityCombinations) {
                    List<Card> hand = new ArrayList<>();
                    hand.addAll(hole);
                    hand.addAll(community);
                    if (hand.size() != 5) {
                        continue;
                    }
                    int value = EvaluationCombination.getHandPower(hand);
                    bestComboValue = Math.max(bestComboValue, value);
                }
            }

            if (bestComboValue > maxValue) {
                maxValue = bestComboValue;
                winners.clear();
                winners.add(player);
            } else if (bestComboValue == maxValue) {
                winners.add(player);
            }
        }
        return winners;
    }

    private List<List<Card>> generateCombinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        generateCombinationsHelper(cards, k, 0, new ArrayList<>(), result);
        return result;
    }

    private void generateCombinationsHelper(List<Card> cards, int k, int start, List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinationsHelper(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    @Override
    public void distributePot(List<Player> winners) {
        if (winners.isEmpty() || sharedBank == 0) return;
        int share = sharedBank / winners.size();
        for (Player winner : winners) {
            winner.setChips(share);
        }
        sharedBank = 0;
    }

    @Override
    public void finishRound() {
        dealerPos = (dealerPos + 1) % players.size();
    }

    @Override
    public void resetRound() {
        desk.clear();
        for (Player p : players) {
            p.setHand(new ArrayList<>());
            p.setFolded(false);
        }
    }

    @Override
    public List<Card> getCommunityCards() {
        return new ArrayList<>(desk);
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public int getPot() {
        return sharedBank;
    }

    @Override
    public int getCurrentBet() {
        return currentBet;
    }

    @Override
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    @Override
    public Map<Player, Integer> getPlayersBets() {
        return new HashMap<>(playersBet);
    }

    @Override
    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        int count = 0;
        int index = currentPlayerIndex;
        do {
            Player p = players.get(index);
            if (!p.isFolded()) {
                currentPlayerIndex = index; // Update to this
                return p;
            }
            index = (index + 1) % players.size();
            count += 1;
        } while (count < players.size());
        return null;
    }

    @Override
    public int getDealerPos() {
        return dealerPos;
    }

    @Override
    public void setDealerPos(int pos) {
        this.dealerPos = pos;
    }
}