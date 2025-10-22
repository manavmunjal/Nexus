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
        // Prevent instantiation
    }

    /**
     * Summarizes predictions for a given group into statistical metrics.
     *
     * @param group Name of the group (e.g., company or product).
     * @param predictions List of prediction results.
     * @param classValues Array of class label values.
     * @return GroupStatistics containing summary statistics.
     */
    public static GroupStatistics summarize(
            final String group,
            final List<PredictionResult> predictions,
            final String[] classValues) {

        final Map<String, Long> counts = new HashMap<>();
        final Map<String, Double> proportions = new HashMap<>();
        final DescriptiveStatistics stats = new DescriptiveStatistics();

        for (final String value : classValues) {
            counts.put(value, 0L);
        }

        for (final PredictionResult result : predictions) {
            stats.addValue(result.expectedScore());
            counts.compute(
                    result.predictedLabel(),
                    (key, current) -> current == null ? 1L : current + 1
            );
        }

        final long total = predictions.size();
        if (total == 0) {
            return new GroupStatistics(
                    group,
                    0,
                    counts,
                    proportions,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }

        for (final Map.Entry<String, Long> entry : counts.entrySet()) {
            proportions.put(entry.getKey(), entry.getValue() / (double) total);
        }

        final double[] scores = stats.getValues();
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

    /**
     * Record holding statistical summaries for a group of predictions.
     *
     * @param group Name of the group.
     * @param totalReviews Total number of reviews.
     * @param labelCounts Count per class label.
     * @param labelProportions Proportion per class label.
     * @param meanScore Mean expected score.
     * @param variance Variance of scores.
     * @param standardDeviation Standard deviation of scores.
     * @param skewness Skewness of score distribution.
     */
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
