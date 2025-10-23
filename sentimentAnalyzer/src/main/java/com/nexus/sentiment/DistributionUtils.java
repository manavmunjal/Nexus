package com.nexus.sentiment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility helpers for working with sentiment distributions.
 */
public final class DistributionUtils {

    private DistributionUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Smooths a probability distribution by adding epsilon to each value and
     * renormalizing.
     *
     * @param distribution The original distribution.
     * @param epsilon Smoothing constant to avoid zeros.
     * @return A new, smoothed distribution.
     */
    public static Map<String, Double> smooth(
            final Map<String, Double> distribution,
            final double epsilon) {

        final Map<String, Double> smoothed = new HashMap<>();
        double total = 0.0;

        for (Map.Entry<String, Double> entry : distribution.entrySet()) {
            final double adjusted = entry.getValue() + epsilon;
            smoothed.put(entry.getKey(), adjusted);
            total += adjusted;
        }

        for (Map.Entry<String, Double> entry : smoothed.entrySet()) {
            smoothed.put(entry.getKey(), entry.getValue() / total);
        }

        return smoothed;
    }

    /**
     * Computes the Kullback-Leibler divergence between two probability
     * distributions.
     *
     * @param p First distribution (expected).
     * @param q Second distribution (actual).
     * @return KL divergence (non-negative).
     */
    public static double klDivergence(
            final Map<String, Double> p,
            final Map<String, Double> q) {

        final Set<String> keys = p.keySet();

        if (!q.keySet().containsAll(keys)) {
            throw new IllegalArgumentException(
                    "Distributions must cover identical labels"
            );
        }

        double divergence = 0.0;
        for (String key : keys) {
            final double pVal = p.get(key);
            final double qVal = q.get(key);
            divergence += pVal * Math.log(pVal / qVal);
        }

        return divergence;
    }

    /**
     * Computes the symmetric KL divergence between two distributions.
     *
     * @param p First distribution.
     * @param q Second distribution.
     * @return Symmetric KL divergence.
     */
    public static double symmetricKlDivergence(
            final Map<String, Double> p,
            final Map<String, Double> q) {

        final double half = 0.5;
        return half * (klDivergence(p, q) + klDivergence(q, p));
    }

    /**
     * Converts a map of label counts to proportions.
     *
     * @param counts A map of label -> count.
     * @return A map of label -> proportion.
     */
    public static Map<String, Double> proportionsFromCounts(
            final Map<String, Long> counts) {

        final Map<String, Double> proportions = new HashMap<>();
        final long total = counts.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        if (total == 0) {
            for (String label : counts.keySet()) {
                proportions.put(label, 0.0);
            }
            return proportions;
        }

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            proportions.put(
                    entry.getKey(),
                    entry.getValue() / (double) total
            );
        }

        return proportions;
    }
}
