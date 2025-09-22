package com.axine.pokercasino.model.game;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.deck.card.Suit;

import java.util.*;
import java.util.stream.Collectors;

public class EvaluationCombination {

    /** Главный метод: возвращает силу руки (чем больше, тем сильнее) */
    public static int getHandPower(List<Card> cards) {
        if (cards == null || cards.size() < 5) {
            throw new IllegalArgumentException("Нужно минимум 5 карт для оценки");
        }

        List<List<Card>> allCombos = generateCombinations(cards, 5);
        int best = 0;
        for (List<Card> combo : allCombos) {
            best = Math.max(best, evaluateFiveCards(combo));
        }
        return best;
    }

    /** Вернёт комбинацию по числовой силе */
    public static Combination getCombinationFromPower(int power) {
        int type = power / 1_000_000;
        return switch (type) {
            case 9 -> Combination.ROYALFLUSH;
            case 8 -> Combination.STRAIGHTFLUSH;
            case 7 -> Combination.QUADS;
            case 6 -> Combination.FULLHOUSE;
            case 5 -> Combination.FLUSH;
            case 4 -> Combination.STRAIGHT;
            case 3 -> Combination.SET;
            case 2 -> Combination.TWOPAIRS;
            case 1 -> Combination.ONEPAIR;
            default -> Combination.HIGHCARD;
        };
    }

    /** Оценка строго 5 карт */
    private static int evaluateFiveCards(List<Card> cards) {
        cards.sort(Comparator.comparingInt(c -> -c.getRank().getValue()));
        Map<Integer, Long> rankCounts = cards.stream()
                .collect(Collectors.groupingBy(c -> c.getRank().getValue(), Collectors.counting()));
        List<Integer> ranksDesc = cards.stream()
                .map(c -> c.getRank().getValue())
                .sorted(Comparator.reverseOrder())
                .toList();

        boolean flush = isFlush(cards);
        boolean straight = isStraight(cards);
        int straightHigh = straight ? getStraightHigh(cards) : 0;

        if (flush && straight) {
            if (straightHigh == 14) return 9_000_000; // Royal
            return 8_000_000 + straightHigh;
        }

        if (rankCounts.containsValue(4L)) {
            int quad = getKeyByValue(rankCounts, 4L);
            int kicker = ranksDesc.stream().filter(r -> r != quad).findFirst().orElse(0);
            return 7_000_000 + quad * 100 + kicker;
        }

        if (rankCounts.containsValue(3L) && rankCounts.size() >= 2) {
            int trips = getHighestKey(rankCounts, 3L);
            int pair = getHighestKey(rankCounts, 2L);
            if (pair > 0) return 6_000_000 + trips * 100 + pair;
        }

        if (flush) {
            return 5_000_000 + encodeKickers(ranksDesc);
        }

        if (straight) {
            return 4_000_000 + straightHigh;
        }

        if (rankCounts.containsValue(3L)) {
            int trips = getHighestKey(rankCounts, 3L);
            List<Integer> kickers = ranksDesc.stream().filter(r -> r != trips).toList();
            return 3_000_000 + trips * 100 + encodeKickers(kickers);
        }

        if (rankCounts.values().stream().filter(v -> v == 2L).count() >= 2) {
            List<Integer> pairs = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() == 2L)
                    .map(Map.Entry::getKey)
                    .sorted(Comparator.reverseOrder())
                    .toList();
            int kicker = ranksDesc.stream().filter(r -> !pairs.contains(r)).findFirst().orElse(0);
            return 2_000_000 + pairs.get(0) * 100 + pairs.get(1) + kicker;
        }

        if (rankCounts.containsValue(2L)) {
            int pair = getHighestKey(rankCounts, 2L);
            List<Integer> kickers = ranksDesc.stream().filter(r -> r != pair).toList();
            return 1_000_000 + pair * 100 + encodeKickers(kickers);
        }

        return encodeKickers(ranksDesc); // High card
    }

    // ================== ВСПОМОГАТЕЛЬНЫЕ ==================

    private static boolean isFlush(List<Card> cards) {
        Suit s = cards.get(0).getSuit();
        return cards.stream().allMatch(c -> c.getSuit() == s);
    }

    private static boolean isStraight(List<Card> cards) {
        List<Integer> values = cards.stream()
                .map(c -> c.getRank().getValue())
                .distinct()
                .sorted()
                .toList();

        // wheel (A-2-3-4-5)
        if (values.containsAll(List.of(2, 3, 4, 5, 14))) return true;

        int consecutive = 1;
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) == values.get(i - 1) + 1) {
                consecutive++;
                if (consecutive == 5) return true;
            } else {
                consecutive = 1;
            }
        }
        return false;
    }

    private static int getStraightHigh(List<Card> cards) {
        List<Integer> values = cards.stream()
                .map(c -> c.getRank().getValue())
                .distinct()
                .sorted()
                .toList();
        if (values.containsAll(List.of(2, 3, 4, 5, 14))) return 5; // wheel
        int consecutive = 1, high = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) == values.get(i - 1) + 1) {
                consecutive++;
                high = values.get(i);
            } else {
                consecutive = 1;
            }
        }
        return high;
    }

    private static int getKeyByValue(Map<Integer, Long> map, long value) {
        return map.entrySet().stream()
                .filter(e -> e.getValue() == value)
                .map(Map.Entry::getKey)
                .findFirst().orElse(0);
    }

    private static int getHighestKey(Map<Integer, Long> map, long value) {
        return map.entrySet().stream()
                .filter(e -> e.getValue() == value)
                .map(Map.Entry::getKey)
                .max(Integer::compare).orElse(0);
    }

    private static int encodeKickers(List<Integer> kickers) {
        int power = 0;
        int factor = 1;
        for (int i = 0; i < Math.min(5, kickers.size()); i++) {
            power += kickers.get(i) * factor;
            factor *= 15; // база чуть больше максимального ранга
        }
        return power;
    }

    private static List<List<Card>> generateCombinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        generateHelper(cards, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static void generateHelper(List<Card> cards, int k, int start,
                                       List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateHelper(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
