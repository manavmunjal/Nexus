package com.nexus.sentiment;

import java.util.Arrays;
import java.util.Map;

/**
 * Holds per-review predictions and derived statistics.
 */
public record PredictionResult(
        String reviewId,
        String company,
        String product,
        String actualLabel,
        String predictedLabel,
        double expectedScore,
        double[] labelDistribution,
        Map<String, Double> labelScores
) {
    public double probabilityFor(String label, String[] classValues) {
        for (int i = 0; i < classValues.length; i++) {
            if (classValues[i].equals(label)) {
                return labelDistribution[i];
            }
        }
        return 0.0;
    }

    public String[] formatProbabilities(String[] classValues) {
        String[] formatted = new String[classValues.length];
        for (int i = 0; i < classValues.length; i++) {
            formatted[i] = classValues[i] + "=" + String.format("%.3f", labelDistribution[i]);
        }
        return formatted;
    }

    public String debugSummary(String[] classValues) {
        return "PredictionResult{" +
                "reviewId='" + reviewId + '\'' +
                ", product='" + product + '\'' +
                ", actual='" + actualLabel + '\'' +
                ", predicted='" + predictedLabel + '\'' +
                ", expectedScore=" + String.format("%.3f", expectedScore) +
                ", distribution=" + Arrays.toString(formatProbabilities(classValues)) +
                '}';
    }
}
