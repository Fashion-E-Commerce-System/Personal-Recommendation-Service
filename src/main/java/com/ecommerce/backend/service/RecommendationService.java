package com.ecommerce.backend.service;

import com.ecommerce.backend.document.Order;
import com.ecommerce.backend.document.OrderItem;
import com.ecommerce.backend.document.Recommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MongoTemplate mongoTemplate;
    private static final String RECOMMENDATIONS_COLLECTION = "recommendations";

    public double generateAndSaveRecommendations(List<Order> training, List<Order> test) {
        LocalDate latestTrainDate = training.stream()
                .map(t -> t.getDate().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        Map<String, List<Integer>> w1 = groupByDate(training, latestTrainDate.minusWeeks(1), latestTrainDate.plusDays(1));
        Map<String, List<Integer>> w2 = groupByDate(training, latestTrainDate.minusWeeks(2), latestTrainDate.minusWeeks(1));
        Map<String, List<Integer>> w3 = groupByDate(training, latestTrainDate.minusWeeks(3), latestTrainDate.minusWeeks(2));
        Map<String, List<Integer>> w4 = groupByDate(training, latestTrainDate.minusWeeks(4), latestTrainDate.minusWeeks(3));

        Map<Integer, Integer> predNext = predictNextItem(training);
        List<Integer> popularItems = getPopularItems(training, latestTrainDate.atStartOfDay());
        Map<Integer, List<Map.Entry<Integer, Long>>> coPurchaseTop = buildCoPurchaseTop(training);

        Set<String> users = new HashSet<>();
        users.addAll(training.stream().map(Order::getUsername).collect(Collectors.toSet()));
        users.addAll(test.stream().map(Order::getUsername).collect(Collectors.toSet()));

        if (mongoTemplate.collectionExists(RECOMMENDATIONS_COLLECTION)) {
            mongoTemplate.dropCollection(RECOMMENDATIONS_COLLECTION);
        }

        List<Recommendation> recommendations = new ArrayList<>();
        Map<String, Set<Integer>> truth = test.stream()
                .collect(Collectors.groupingBy(Order::getUsername,
                        Collectors.flatMapping(order -> order.getProducts().stream().map(p -> Integer.parseInt(p.getProductId())), Collectors.toSet())));

        Map<String, List<Integer>> predictions = new HashMap<>();

        for (String user : users) {
            List<Integer> userOutput = new ArrayList<>();
            userOutput.addAll(topFreq(w1.getOrDefault(user, Collections.emptyList()), 12));
            userOutput.addAll(topFreq(w2.getOrDefault(user, Collections.emptyList()), 12));
            userOutput.addAll(topFreq(w3.getOrDefault(user, Collections.emptyList()), 12));
            userOutput.addAll(topFreq(w4.getOrDefault(user, Collections.emptyList()), 12));
            userOutput = userOutput.stream().distinct().collect(Collectors.toList());

            List<Integer> nextPreds = new ArrayList<>();
            for (Integer item : userOutput) {
                Integer next = predNext.get(item);
                if (next != null && !userOutput.contains(next)) nextPreds.add(next);
            }
            userOutput.addAll(nextPreds);

            Set<Integer> coCandidates = new LinkedHashSet<>();
            for (Integer item : userOutput) {
                List<Map.Entry<Integer, Long>> list = coPurchaseTop.get(item);
                if (list != null) {
                    for (Map.Entry<Integer, Long> e : list) {
                        if (!userOutput.contains(e.getKey())) coCandidates.add(e.getKey());
                    }
                }
            }
            userOutput.addAll(coCandidates);

            if (userOutput.size() < 24) {
                for (Integer p : popularItems) {
                    if (userOutput.size() >= 24) break;
                    if (!userOutput.contains(p)) userOutput.add(p);
                }
            }

            List<Integer> top12 = userOutput.stream().distinct().limit(12).collect(Collectors.toList());
            predictions.put(user, top12);
            for (Integer articleId : top12) {
                recommendations.add(new Recommendation(null, user, articleId));
            }
        }

        mongoTemplate.insertAll(recommendations);
        double map = mapAtK(predictions, truth, 12);
        return map;
    }

    private Map<String, List<Integer>> groupByDate(List<Order> orders, LocalDate start, LocalDate end) {
        return orders.stream()
                .filter(t -> {
                    LocalDate d = t.getDate().toLocalDate();
                    return !d.isBefore(start) && d.isBefore(end);
                })
                .collect(Collectors.groupingBy(Order::getUsername,
                        Collectors.flatMapping(order -> order.getProducts().stream()
                                .map(p -> Integer.parseInt(p.getProductId())), Collectors.toList())));
    }

    private Map<Integer, Integer> predictNextItem(List<Order> orders) {
        Map<Integer, Map<Integer, Integer>> nextItemCounts = new HashMap<>();

        for (Order order : orders) {
            List<OrderItem> items = order.getProducts();
            if (items != null && items.size() > 1) {
                for (int i = 0; i < items.size() - 1; i++) {
                    try {
                        int currentItemId = Integer.parseInt(items.get(i).getProductId());
                        int nextItemId = Integer.parseInt(items.get(i + 1).getProductId());

                        nextItemCounts.computeIfAbsent(currentItemId, k -> new HashMap<>())
                                .merge(nextItemId, 1, Integer::sum);
                    } catch (NumberFormatException e) {
                        // Ignore items with non-integer product IDs
                    }
                }
            }
        }

        Map<Integer, Integer> predictions = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : nextItemCounts.entrySet()) {
            entry.getValue().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(maxEntry -> predictions.put(entry.getKey(), maxEntry.getKey()));
        }

        return predictions;
    }

    private List<Integer> getPopularItems(List<Order> orders, LocalDateTime endDate) {
        Map<Integer, Double> score = orders.stream()
                .flatMap(order -> order.getProducts().stream()
                        .map(item -> new AbstractMap.SimpleEntry<>(item, order.getDate())))
                .collect(Collectors.groupingBy(entry -> Integer.parseInt(entry.getKey().getProductId()),
                        Collectors.summingDouble(entry -> {
                            long days = Duration.between(entry.getValue(), endDate).toDays();
                            return 1.0 / (days == 0 ? 1.0 : days);
                        })));
        return score.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Map<Integer, List<Map.Entry<Integer, Long>>> buildCoPurchaseTop(List<Order> orders) {
        Map<String, Set<Integer>> userToItems = orders.stream()
                .collect(Collectors.groupingBy(Order::getUsername,
                        Collectors.flatMapping(order -> order.getProducts().stream()
                                .map(p -> Integer.parseInt(p.getProductId())), Collectors.toSet())));

        Map<Integer, Map<Integer, Long>> coCounts = new HashMap<>();
        for (Set<Integer> items : userToItems.values()) {
            List<Integer> list = new ArrayList<>(items);
            for (int i = 0; i < list.size(); i++) {
                for (int j = 0; j < list.size(); j++) {
                    if (i == j) continue;
                    int a = list.get(i);
                    int b = list.get(j);
                    coCounts.computeIfAbsent(a, k -> new HashMap<>()) 
                            .merge(b, 1L, Long::sum);
                }
            }
        }

        Map<Integer, List<Map.Entry<Integer, Long>>> top = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Long>> e : coCounts.entrySet()) {
            List<Map.Entry<Integer, Long>> sorted = e.getValue().entrySet().stream()
                    .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                    .limit(20)
                    .collect(Collectors.toList());
            top.put(e.getKey(), sorted);
        }
        return top;
    }

    private List<Integer> topFreq(List<Integer> items, int limit) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        return items.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private double apAtK(List<Integer> recs, Set<Integer> truths, int k) {
        List<Integer> unique = recs.stream().distinct().limit(k).collect(Collectors.toList());
        if (truths == null || truths.isEmpty()) return 0.0;
        int tp = 0;
        double sum = 0.0;
        for (int i = 0; i < unique.size(); i++) {
            if (truths.contains(unique.get(i))) {
                tp++;
                sum += (double) tp / (i + 1);
            }
        }
        return sum / Math.min(truths.size(), k);
    }

    private double mapAtK(Map<String, List<Integer>> preds, Map<String, Set<Integer>> truths, int k) {
        List<Double> aps = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> e : preds.entrySet()) {
            aps.add(apAtK(e.getValue(), truths.getOrDefault(e.getKey(), Collections.emptySet()), k));
        }
        return aps.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    public List<Recommendation> getRecommendationsForUser(String username, List<Integer> productIds) {
        Query query = Query.query(Criteria.where("username").is(username));
        if (productIds != null && !productIds.isEmpty()) {
            query.addCriteria(Criteria.where("productId").in(productIds));
        }
        return mongoTemplate.find(query, Recommendation.class);
    }
}


