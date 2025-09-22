package com.axine.pokercasino.controller;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.player.Event;
import com.axine.pokercasino.model.player.Player;
import com.axine.pokercasino.model.player.PlayerAction;
import com.axine.pokercasino.model.player.players.HumanPlayer;
import com.axine.pokercasino.service.GameService;
import com.axine.pokercasino.model.game.Combination;
import com.axine.pokercasino.model.game.EvaluationCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RestController
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    @Autowired
    private GameService gameService;

    @GetMapping("/")
    public String home(Model model) {
        if (!gameService.isGameStarted()) {
            return "index";
        }
        return "redirect:/game.html";
    }

    @GetMapping("/game")
    public String game(Model model) {
        return "game";
    }

    @PostMapping("/api/start")
    public ResponseEntity<Map<String, Object>> startGame(@RequestBody Map<String, Object> payload) {
        try {
            String gameType = (String) payload.get("gameType");
            String deckType = (String) payload.getOrDefault("deckType", "standard");
            List<Map<String, String>> players = (List<Map<String, String>>) payload.getOrDefault("players", List.of());
            int smallBlind = Integer.parseInt(payload.getOrDefault("smallBlind", "10").toString());
            int bigBlind = Integer.parseInt(payload.getOrDefault("bigBlind", "20").toString());

            gameService.startGame(gameType, deckType, players, smallBlind, bigBlind);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка при запуске игры: " + e.getClass().getSimpleName();
            logger.error("Ошибка запуска игры: {}", errorMessage, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", errorMessage));
        }
    }

    @GetMapping("/api/state")
    public ResponseEntity<Map<String, Object>> getState() {
        try {
            if (!gameService.isGameStarted()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Игра не начата"));
            }
            gameService.advanceGame(null);
            Map<String, Object> state = new HashMap<>();
            state.put("pot", gameService.getPot());
            state.put("communityCards", gameService.getCommunityCards() != null ?
                    gameService.getCommunityCards().stream()
                            .map(c -> Map.of("suit", c.getSuit().getShortName(), "rank", c.getRank().getShortName()))
                            .toList() : List.of());
            List<Map<String, Object>> playersList = gameService.getPlayers() != null ?
                    gameService.getPlayers().stream()
                            .map(p -> {
                                Map<String, Object> pMap = new HashMap<>(Map.of("name", p.getName(), "chips", p.getChips(), "bet", gameService.getPlayersBets().getOrDefault(p, 0), "folded", p.isFolded()));
                                return pMap;
                            }).toList() : List.of();
            state.put("players", playersList);
            state.put("hand", gameService.getHumanPlayer() != null && gameService.getHumanPlayer().getHand() != null ?
                    gameService.getHumanPlayer().getHand().stream()
                            .map(c -> Map.of("suit", c.getSuit().getShortName(), "rank", c.getRank().getShortName()))
                            .toList() : List.of());
            state.put("yourTurn", gameService.getCurrentPlayer() != null && gameService.getCurrentPlayer() instanceof HumanPlayer && !gameService.isBettingComplete());
            state.put("message", gameService.getMessage() != null ? gameService.getMessage() : "");
            state.put("roundEnded", gameService.checkRoundCompletion());
            state.put("winners", gameService.checkRoundCompletion() && gameService.getWinners() != null ?
                    gameService.getWinners().stream().map(Player::getName).toList() : List.of());
            state.put("currentBet", gameService.getCurrentBet());
            int humanBet = gameService.getPlayersBets().getOrDefault(gameService.getHumanPlayer(), 0);
            state.put("toCall", gameService.getCurrentBet() - humanBet);
            state.put("success", true);

            if ((boolean) state.get("roundEnded")) {
                List<Map<String, Object>> updatedPlayers = (List<Map<String, Object>>) state.get("players");
                List<Card> community = gameService.getCommunityCards();
                for (Map<String, Object> pMap : updatedPlayers) {
                    String name = (String) pMap.get("name");
                    Player p = gameService.getPlayers().stream().filter(pp -> pp.getName().equals(name)).findFirst().orElse(null);
                    if (p != null && p.getHand() != null && !p.isFolded()) {
                        List<Card> allCards = new ArrayList<>(p.getHand());
                        allCards.addAll(community);
                        int power = EvaluationCombination.getHandPower(allCards);
                        Combination combo = EvaluationCombination.getCombinationFromPower(power);
                        pMap.put("hand", p.getHand().stream().map(c -> Map.of("suit", c.getSuit().getShortName(), "rank", c.getRank().getShortName())).toList());
                        pMap.put("combo", combo.getName());
                    }
                }
            }

            return ResponseEntity.ok(state);
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка при получении состояния: " + e.getClass().getSimpleName();
            logger.error("Ошибка получения состояния: {}", errorMessage, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", errorMessage));
        }
    }

    @PostMapping("/api/action")
    public ResponseEntity<Map<String, Object>> action(@RequestBody Map<String, String> payload) {
        try {
            if (!gameService.isGameStarted()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Игра не начата"));
            }
            String eventStr = payload.get("action");
            if ("raise".equalsIgnoreCase(eventStr)) {
                eventStr = "bet";
            }
            if (eventStr == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Действие не указано"));
            }
            int amount = payload.containsKey("amount") ? Integer.parseInt(payload.get("amount")) : 0;
            Event e = Event.valueOf(eventStr.toUpperCase());
            PlayerAction action = new PlayerAction(e, amount);
            gameService.submitHumanAction(action);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Недопустимое действие: " + e.getClass().getSimpleName();
            logger.error("Ошибка действия: {}", errorMessage, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", errorMessage));
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка при выполнении действия: " + e.getClass().getSimpleName();
            logger.error("Ошибка действия: {}", errorMessage, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "error", errorMessage));
        }
    }

    @PostMapping("/api/continue")
    public ResponseEntity<Map<String, Object>> continueGame() {
        try {
            if (!gameService.isGameStarted()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Игра не начата"));
            }
            gameService.continueGame();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка при продолжении игры: " + e.getClass().getSimpleName();
            logger.error("Ошибка продолжения игры: {}", errorMessage, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", errorMessage));
        }
    }
}