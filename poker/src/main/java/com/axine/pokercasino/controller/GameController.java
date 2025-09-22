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

            // Продвигаем игру (как у тебя было)
            gameService.advanceGame(null);

            Map<String, Object> state = new HashMap<>();
            state.put("pot", gameService.getPot());

            // community cards -> JSON-friendly list
            List<Map<String, String>> communityJson = new ArrayList<>();
            List<Card> community = gameService.getCommunityCards();
            if (community != null) {
                for (Card c : community) {
                    communityJson.add(Map.of("suit", c.getSuit().getShortName(), "rank", c.getRank().getShortName()));
                }
            }
            state.put("communityCards", communityJson);

            // players - создаём изменяемый список карт
            List<Map<String, Object>> playersList = new ArrayList<>();
            if (gameService.getPlayers() != null) {
                for (Player p : gameService.getPlayers()) {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("name", p.getName());
                    pMap.put("chips", p.getChips());
                    pMap.put("bet", gameService.getPlayersBets().getOrDefault(p, 0));
                    pMap.put("folded", p.isFolded());
                    // не добавляем hand/ combo сейчас — сделаем это ниже, если раунд закончился
                    playersList.add(pMap);
                }
            }
            state.put("players", playersList);

            // твоя рука
            List<Map<String, String>> myHandJson = new ArrayList<>();
            if (gameService.getHumanPlayer() != null && gameService.getHumanPlayer().getHand() != null) {
                for (Card c : gameService.getHumanPlayer().getHand()) {
                    myHandJson.add(Map.of("suit", c.getSuit().getShortName(), "rank", c.getRank().getShortName()));
                }
            }
            state.put("hand", myHandJson);

            // баланс игрока
            state.put("yourChips", gameService.getHumanPlayer() != null ? gameService.getHumanPlayer().getChips() : 0);

            // твой ход?
            state.put("yourTurn", gameService.getCurrentPlayer() != null
                    && gameService.getCurrentPlayer() instanceof HumanPlayer
                    && !gameService.isBettingComplete());

            state.put("message", gameService.getMessage() != null ? gameService.getMessage() : "");

            boolean roundEnded = gameService.checkRoundCompletion();
            state.put("roundEnded", roundEnded);

            // winners names (если есть)
            List<String> winnerNames = new ArrayList<>();
            if (roundEnded && gameService.getWinners() != null) {
                for (Player w : gameService.getWinners()) {
                    winnerNames.add(w.getName());
                }
            }
            state.put("winners", winnerNames);

            state.put("currentBet", gameService.getCurrentBet());
            int humanBet = gameService.getPlayersBets().getOrDefault(gameService.getHumanPlayer(), 0);
            state.put("toCall", gameService.getCurrentBet() - humanBet);
            state.put("success", true);

            // Если раунд завершён — дополняем игроков их картами и комбинацией
            if (roundEnded) {
                // Обход именно playersList (тот же объект, что в state)
                for (Map<String, Object> pMap : playersList) {
                    String name = (String) pMap.get("name");
                    // Находим соответствующий Player по имени
                    Player realPlayer = null;
                    if (gameService.getPlayers() != null) {
                        for (Player pp : gameService.getPlayers()) {
                            if (pp.getName().equals(name)) {
                                realPlayer = pp;
                                break;
                            }
                        }
                    }

                    if (realPlayer == null) continue;

                    // Добавляем hand (даже если игрок фолднул — можно не показывать, но здесь показываем только если не фолд)
                    if (realPlayer.getHand() != null && !realPlayer.isFolded()) {
                        List<Map<String, String>> handJson = new ArrayList<>();
                        for (Card c : realPlayer.getHand()) {
                            handJson.add(Map.of("suit", c.getSuit().getShortName(), "rank", c.getRank().getShortName()));
                        }
                        pMap.put("hand", handJson);

                        // Составляем allCards = hand + community и считаем комбинацию, только если >=5 карт
                        List<Card> allCards = new ArrayList<>(realPlayer.getHand());
                        if (community != null) allCards.addAll(community);

                        if (allCards.size() >= 5) {
                            try {
                                int power = EvaluationCombination.getHandPower(allCards);
                                Combination combo = EvaluationCombination.getCombinationFromPower(power);
                                pMap.put("combo", combo.getName());
                            } catch (Exception ex) {
                                // Если вдруг EvaluationCombination кидает исключение — не ломаем JSON, просто не ставим combo
                                pMap.put("combo", "");
                            }
                        } else {
                            pMap.put("combo", ""); // недостаточно карт для полной оценки
                        }
                    }

                    // Помечаем победителя (если есть)
                    boolean isWinner = false;
                    for (String wn : winnerNames) {
                        if (wn.equals(name)) { isWinner = true; break; }
                    }
                    pMap.put("winner", isWinner);
                }
                // state уже содержит playersList (мы модифицировали его), поэтому ничего дополнительно put делать не нужно
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