package com.nexus.sentiment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility helpers for working with sentiment distributions.
 */
public final class DistributionUtils {
    private DistributionUtils() {
    }

    public static Map<String, Double> smooth(Map<String, Double> distribution, double epsilon) {
        Map<String, Double> smoothed = new HashMap<>();
        double total = 0.0;
        for (Map.Entry<String, Double> entry : distribution.entrySet()) {
            double adjusted = entry.getValue() + epsilon;
            smoothed.put(entry.getKey(), adjusted);
            total += adjusted;
        }
        for (Map.Entry<String, Double> entry : smoothed.entrySet()) {
            smoothed.put(entry.getKey(), entry.getValue() / total);
        }
        return smoothed;
    }

    public static double klDivergence(Map<String, Double> p, Map<String, Double> q) {
        Set<String> keys = p.keySet();
        if (!q.keySet().containsAll(keys)) {
            throw new IllegalArgumentException("Distributions must cover identical labels");
        }
        double divergence = 0.0;
        for (String key : keys) {
            double pVal = p.get(key);
            double qVal = q.get(key);
            divergence += pVal * Math.log(pVal / qVal);
        }
        return divergence;
    }

    public static double symmetricKlDivergence(Map<String, Double> p, Map<String, Double> q) {
        return 0.5 * (klDivergence(p, q) + klDivergence(q, p));
    }

    public static Map<String, Double> proportionsFromCounts(Map<String, Long> counts) {
        Map<String, Double> proportions = new HashMap<>();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            counts.keySet().forEach(label -> proportions.put(label, 0.0));
            return proportions;
        }
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            proportions.put(entry.getKey(), entry.getValue() / (double) total);
        }
        return proportions;
    }
}
