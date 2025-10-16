package com.nexus.sentiment;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes aggregate sentiment statistics for a collection of predictions.
 */
public final class SentimentStatistics {
    private SentimentStatistics() {
    }

    public static GroupStatistics summarize(String group, List<PredictionResult> predictions, String[] classValues) {
        Map<String, Long> counts = new HashMap<>();
        Map<String, Double> proportions = new HashMap<>();
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (String value : classValues) {
            counts.put(value, 0L);
        }

        for (PredictionResult result : predictions) {
            stats.addValue(result.expectedScore());
            counts.compute(result.predictedLabel(), (key, current) -> current == null ? 1L : current + 1);
        }

        long total = predictions.size();
        if (total == 0) {
            return new GroupStatistics(group, 0, counts, proportions, 0.0, 0.0, 0.0, 0.0);
        }

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            proportions.put(entry.getKey(), entry.getValue() / (double) total);
        }

        double[] scores = stats.getValues();
        double skewness = new Skewness().evaluate(scores);
        if (Double.isNaN(skewness)) {
            skewness = 0.0;
        }

    return new GroupStatistics(
        group,
        total,
        Map.copyOf(counts),
        Map.copyOf(proportions),
                stats.getMean(),
                stats.getVariance(),
                stats.getStandardDeviation(),
                skewness
        );
    }

    public record GroupStatistics(
            String group,
            long totalReviews,
            Map<String, Long> labelCounts,
            Map<String, Double> labelProportions,
            double meanScore,
            double variance,
            double standardDeviation,
            double skewness
    ) {
    }
}
