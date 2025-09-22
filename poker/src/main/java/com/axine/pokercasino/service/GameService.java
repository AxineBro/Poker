package com.axine.pokercasino.service;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.deck.Deck;
import com.axine.pokercasino.model.deck.DeckFactory;
import com.axine.pokercasino.model.deck.deckFactory.ShortenedDeckFactory;
import com.axine.pokercasino.model.deck.deckFactory.StandardDeckFactory;
import com.axine.pokercasino.model.game.Round;
import com.axine.pokercasino.model.game.RoundFactory;
import com.axine.pokercasino.model.game.Stage;
import com.axine.pokercasino.model.game.gameFactory.OmahaHoldemRoundFactory;
import com.axine.pokercasino.model.game.gameFactory.TexasHoldemRoundFactory;
import com.axine.pokercasino.model.player.Player;
import com.axine.pokercasino.model.player.PlayerAction;
import com.axine.pokercasino.model.player.PlayerFactory;
import com.axine.pokercasino.model.player.playerFactory.BotAIPlayerFactory;
import com.axine.pokercasino.model.player.playerFactory.BotRandomPlayerFactory;
import com.axine.pokercasino.model.player.playerFactory.HumanPlayerFactory;
import com.axine.pokercasino.model.player.players.HumanPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SessionScope
public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private Round round;
    private List<Player> players;
    private Deck deck;
    private RoundFactory roundFactory;
    private boolean gameStarted = false;
    private String message = "";
    private Player humanPlayer;
    private boolean roundEnded = false;
    private int smallBlind;
    private int bigBlind;
    private int globalDealerPos = 0;

    public void startGame(String gameType, String deckType, List<Map<String, String>> playerConfigs, int smallBlind, int bigBlind) {
        try {
            if (gameType == null || deckType == null) {
                throw new IllegalArgumentException("Тип игры или колоды не указан");
            }
            DeckFactory deckFactory = deckType.equals("shortened") ? new ShortenedDeckFactory() : new StandardDeckFactory();
            deck = deckFactory.createDeck();
            if (deck == null) throw new IllegalStateException("Не удалось создать колоду");

            players = new ArrayList<>();
            PlayerFactory humanFactory = new HumanPlayerFactory();
            humanPlayer = humanFactory.createPlayer("You", 1000);
            if (humanPlayer == null) throw new IllegalStateException("Не удалось создать игрока");
            players.add(humanPlayer);

            for (Map<String, String> config : playerConfigs) {
                if (config == null) continue;
                String type = config.getOrDefault("type", "random");
                String name = config.getOrDefault("name", "Bot" + (players.size() + 1));
                PlayerFactory botFactory = type.equals("random") ? new BotRandomPlayerFactory() : new BotAIPlayerFactory();
                Player bot = botFactory.createPlayer(name, 1000);
                if (bot != null) players.add(bot);
            }

            if (players.size() < 2) throw new IllegalArgumentException("Нужно минимум 2 игрока");

            roundFactory = gameType.equals("omaha") ? new OmahaHoldemRoundFactory() : new TexasHoldemRoundFactory();
            round = roundFactory.createRound(players, deck, smallBlind, bigBlind);
            if (round == null) throw new IllegalStateException("Не удалось создать раунд");

            this.smallBlind = smallBlind;
            this.bigBlind = bigBlind;
            globalDealerPos = 0;
            round.setDealerPos(globalDealerPos);

            roundFactory.startRound(round);
            roundFactory.distributeCards(round);
            gameStarted = true;
            roundEnded = false;
            message = "Игра началась! Тип: " + gameType + ", Колода: " + deckType + ", Блайнды: " + smallBlind + "/" + bigBlind;
            advanceGame(null);
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка при запуске игры: " + e.getClass().getSimpleName();
            logger.error("Ошибка при запуске игры: {}", errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    public void advanceGame(PlayerAction humanAction) {
        if (!gameStarted) {
            message = "Игра не начата";
            return;
        }

        try {
            if (round == null) {
                throw new IllegalStateException("Раунд не инициализирован");
            }
            if (roundFactory.checkRoundCompletion(round)) {
                List<Player> winners = roundFactory.getWinners(round);
                roundFactory.distributePot(winners, round);
                message = "Раунд завершён. Победители: " + (winners.isEmpty() ? "Нет" : winners.stream().map(Player::getName).collect(Collectors.joining(", ")));
                roundEnded = true;
            } else {
                if (roundFactory.isBettingComplete(round)) {
                    roundFactory.advanceToNextStage(round);
                    if (!roundFactory.checkRoundCompletion(round)) {
                        roundFactory.dealCommunityCards(round.getStage(), round);
                        message = "Стадия: " + round.getStage();
                    }
                } else {
                    roundFactory.manageBettingRound(round, humanAction);
                }
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка в ходе игры: " + e.getClass().getSimpleName();
            logger.error("Ошибка в advanceGame: {}", errorMessage, e);
            message = "Ошибка: " + errorMessage;
        }
    }

    public void continueGame() {
        if (!gameStarted || !roundEnded) {
            message = "Невозможно продолжить: игра не начата или раунд не завершён";
            return;
        }
        try {
            if (round == null || deck == null) {
                throw new IllegalStateException("Раунд или колода не инициализированы");
            }
            globalDealerPos = (globalDealerPos + 1) % players.size();
            deck.initialize();
            deck.shuffle();
            round = roundFactory.createRound(players, deck, smallBlind, bigBlind);
            round.setDealerPos(globalDealerPos);
            roundFactory.startRound(round);
            roundFactory.distributeCards(round);
            roundEnded = false;
            message = "Новый раунд начался.";
            advanceGame(null);
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка при продолжении игры: " + e.getClass().getSimpleName();
            logger.error("Ошибка в continueGame: {}", errorMessage, e);
            message = "Ошибка: " + errorMessage;
            throw new IllegalStateException(errorMessage, e);
        }
    }

    public void submitHumanAction(PlayerAction action) {
        try {
            if (action == null) throw new IllegalArgumentException("Действие не указано");
            if (round == null) throw new IllegalStateException("Раунд не инициализирован");
            if (this.getCurrentPlayer() == null) throw new IllegalStateException("Текущий игрок не определён");
            if (!(this.getCurrentPlayer() instanceof HumanPlayer)) throw new IllegalStateException("Не очередь человеческого игрока");
            roundFactory.submitHumanAction(round, action);
            message = "Действие выполнено: " + action;
            advanceGame(action);
        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Недопустимое действие: " + e.getClass().getSimpleName();
            logger.error("Ошибка в submitHumanAction: {}", errorMessage, e);
            message = "Ошибка: " + errorMessage;
            throw new IllegalArgumentException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка при выполнении действия: " + e.getClass().getSimpleName();
            logger.error("Ошибка в submitHumanAction: {}", errorMessage, e);
            message = "Ошибка: " + errorMessage;
            throw new IllegalStateException(errorMessage, e);
        }
    }

    public List<Card> getCommunityCards() {
        return roundFactory != null ? roundFactory.getCommunityCards(round) : List.of();
    }

    public Stage getStage() {
        return roundFactory != null ? roundFactory.getStage(round) : Stage.PREFLOP;
    }

    public int getPot() {
        return roundFactory != null ? roundFactory.getPot(round) : 0;
    }

    public int getCurrentBet() {
        return roundFactory != null ? roundFactory.getCurrentBet(round) : 0;
    }

    public List<Player> getPlayers() {
        return roundFactory != null ? roundFactory.getPlayers(round) : List.of();
    }

    public Map<Player, Integer> getPlayersBets() {
        return roundFactory != null ? roundFactory.getPlayersBets(round) : Map.of();
    }

    public Player getCurrentPlayer() {
        return roundFactory != null ? roundFactory.getCurrentPlayer(round) : null;
    }

    public boolean isBettingComplete() {
        return roundFactory != null ? roundFactory.isBettingComplete(round) : true;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public Player getHumanPlayer() {
        return humanPlayer;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean checkRoundCompletion() {
        return roundEnded || (roundFactory != null && roundFactory.checkRoundCompletion(round));
    }

    public List<Player> getWinners() {
        if (!checkRoundCompletion()) return List.of();
        return roundFactory != null ? roundFactory.getWinners(round) : List.of();
    }
}