package com.axine.pokercasino.model.game;

import com.axine.pokercasino.model.deck.Card;
import com.axine.pokercasino.model.deck.card.Rank;
import com.axine.pokercasino.model.deck.card.Suit;

import java.util.*;
import java.util.stream.Collectors;

public class EvaluationCombination {

    public static int getHandPower(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            throw new IllegalArgumentException("Card list cannot be null or empty");
        }

        Map<Suit, List<Card>> suitGroups = groupBySuit(cards);
        Map<Integer, Integer> rankCounts = groupByRank(cards);
        List<Integer> sortedRanks = rankCounts.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());

        if (hasRoyalFlush(suitGroups)) {
            return 9000000;
        } else if (hasStraightFlush(suitGroups)) {
            return 8000000 + getStraightHigh(suitGroups);
        } else if (hasQuads(rankCounts)) {
            int quadRank = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() >= 4)
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(0);
            return 7000000 + quadRank * 1000 + getKicker(cards, quadRank);
        } else if (hasFullHouse(rankCounts)) {
            int threeRank = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() >= 3)
                    .map(Map.Entry::getKey)
                    .max(Integer::compare).orElse(0);
            int pairRank = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() >= 2 && e.getKey() != threeRank)
                    .map(Map.Entry::getKey)
                    .max(Integer::compare).orElse(0);
            return 6000000 + threeRank * 1000 + pairRank * 100;
        } else if (hasFlush(suitGroups)) {
            return 5000000 + getHighCard(cards);
        } else if (hasStraight(sortedRanks)) {
            return 4000000 + getStraightHighValue(sortedRanks);
        } else if (hasThreeOfAKind(rankCounts)) {
            int threeRank = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() == 3)
                    .map(Map.Entry::getKey)
                    .max(Integer::compare).orElse(0);
            return 3000000 + threeRank * 1000 + getKicker(cards, threeRank);
        } else if (hasTwoPairs(rankCounts)) {
            List<Integer> pairs = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() == 2)
                    .map(Map.Entry::getKey)
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
            return 2000000 + pairs.get(0) * 1000 + pairs.get(1) * 100 + getKicker(cards, pairs.get(0), pairs.get(1));
        } else if (hasOnePair(rankCounts)) {
            int pairRank = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() == 2)
                    .map(Map.Entry::getKey)
                    .max(Integer::compare).orElse(0);
            return 1000000 + pairRank * 1000 + getKicker(cards, pairRank);
        } else {
            return getHighCard(cards);
        }
    }

    public static Combination getCombinationFromPower(int power) {
        int type = power / 1000000;
        if (type < 0 || type >= Combination.values().length) return Combination.HIGHCARD;
        return Combination.values()[type];
    }

    private static Map<Suit, List<Card>> groupBySuit(List<Card> cards) {
        Map<Suit, List<Card>> map = new HashMap<>();
        for (Card c : cards) {
            map.computeIfAbsent(c.getSuit(), k -> new ArrayList<>()).add(c);
        }
        return map;
    }

    private static Map<Integer, Integer> groupByRank(List<Card> cards) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Card c : cards) {
            map.merge(c.getRank().getValue(), 1, Integer::sum);
        }
        return map;
    }

    private static boolean hasRoyalFlush(Map<Suit, List<Card>> suitGroups) {
        for (List<Card> suitCards : suitGroups.values()) {
            if (suitCards.size() < 5) continue;

            suitCards.sort(Comparator.comparingInt((Card c) -> -c.getRank().getValue()));
            if (suitCards.get(0).getRank().getValue() == 14 &&
                    suitCards.get(1).getRank().getValue() == 13 &&
                    suitCards.get(2).getRank().getValue() == 12 &&
                    suitCards.get(3).getRank().getValue() == 11 &&
                    suitCards.get(4).getRank().getValue() == 10) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasStraightFlush(Map<Suit, List<Card>> suitGroups) {
        for (List<Card> suitCards : suitGroups.values()) {
            if (suitCards.size() < 5) continue;

            List<Integer> ranks = suitCards.stream()
                    .mapToInt(c -> c.getRank().getValue())
                    .distinct()
                    .boxed()
                    .sorted()
                    .collect(Collectors.toList());

            if (hasConsecutiveStraight(ranks)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasQuads(Map<Integer, Integer> rankCounts) {
        return rankCounts.values().stream().anyMatch(count -> count >= 4);
    }

    private static boolean hasFullHouse(Map<Integer, Integer> rankCounts) {
        boolean hasThree = false;
        boolean hasPair = false;
        for (int count : rankCounts.values()) {
            if (count >= 3) {
                if (hasThree) {
                    return true;
                }
                hasThree = true;
            } else if (count >= 2) {
                if (hasPair) {
                } else {
                    hasPair = true;
                }
            }
        }
        return hasThree && hasPair;
    }

    private static boolean hasFlush(Map<Suit, List<Card>> suitGroups) {
        return suitGroups.values().stream().anyMatch(list -> list.size() >= 5);
    }

    private static boolean hasStraight(List<Integer> sortedUniqueRanks) {
        return hasConsecutiveStraight(sortedUniqueRanks);
    }

    private static boolean hasThreeOfAKind(Map<Integer, Integer> rankCounts) {
        return rankCounts.values().stream().anyMatch(count -> count == 3);
    }

    private static boolean hasTwoPairs(Map<Integer, Integer> rankCounts) {
        long pairCount = rankCounts.values().stream().filter(count -> count == 2).count();
        return pairCount >= 2;
    }

    private static boolean hasOnePair(Map<Integer, Integer> rankCounts) {
        long pairCount = rankCounts.values().stream().filter(count -> count == 2).count();
        return pairCount == 1;
    }

    private static boolean hasConsecutiveStraight(List<Integer> sortedUniqueRanks) {
        if (sortedUniqueRanks.size() < 5) return false;

        int consecutive = 1;
        for (int i = 1; i < sortedUniqueRanks.size(); i++) {
            if (sortedUniqueRanks.get(i) - sortedUniqueRanks.get(i - 1) == 1) {
                consecutive++;
                if (consecutive >= 5) return true;
            } else {
                consecutive = 1;
            }
        }

        Set<Integer> rankSet = new HashSet<>(sortedUniqueRanks);
        if (rankSet.contains(14) && rankSet.contains(2) && rankSet.contains(3) &&
                rankSet.contains(4) && rankSet.contains(5)) {
            return true;
        }

        return false;
    }

    private static int getHighCard(List<Card> cards) {
        List<Integer> ranks = cards.stream()
                .mapToInt(c -> c.getRank().getValue())
                .boxed()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        int power = 0;
        for (int i = 0; i < Math.min(ranks.size(), 5); i++) {
            power += ranks.get(i) * Math.pow(10, 4 - i * 2);
        }
        return power;
    }

    private static int getStraightHigh(Map<Suit, List<Card>> suitGroups) {
        for (List<Card> suitCards : suitGroups.values()) {
            if (suitCards.size() < 5) continue;
            List<Integer> ranks = suitCards.stream()
                    .mapToInt(c -> c.getRank().getValue())
                    .distinct()
                    .boxed()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
            if (hasConsecutiveStraight(ranks)) {
                return ranks.get(0);
            }
            if (new HashSet<>(ranks).containsAll(List.of(14, 2, 3, 4, 5))) {
                return 5; // Wheel
            }
        }
        return 0;
    }

    private static int getStraightHighValue(List<Integer> sortedRanks) {
        int consecutive = 1;
        int high = sortedRanks.get(0);
        for (int i = 1; i < sortedRanks.size(); i++) {
            if (sortedRanks.get(i) - sortedRanks.get(i - 1) == 1) {
                consecutive++;
                if (consecutive >= 5) {
                    high = sortedRanks.get(i - 4);
                }
            } else {
                consecutive = 1;
            }
        }
        if (new HashSet<>(sortedRanks).containsAll(List.of(14, 2, 3, 4, 5))) {
            return 5; // Wheel
        }
        return high;
    }

    private static int getKicker(List<Card> cards, Integer... excludeRanks) {
        List<Integer> ranks = cards.stream()
                .mapToInt(c -> c.getRank().getValue())
                .filter(r -> !Arrays.asList(excludeRanks).contains(r))
                .boxed()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        int power = 0;
        for (int i = 0; i < Math.min(ranks.size(), 3); i++) {
            power += ranks.get(i) * Math.pow(10, 2 - i * 2);
        }
        return power;
    }
}