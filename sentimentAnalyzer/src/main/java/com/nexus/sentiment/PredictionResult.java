package com.nexus.sentiment;

import java.util.Arrays;
import java.util.Map;

/**
 * Holds per-review predictions and derived statistics.
 *
 * @param reviewId The unique ID of the review
 * @param company The company associated with the review
 * @param product The product associated with the review
 * @param actualLabel The actual sentiment label
 * @param predictedLabel The predicted sentiment label
 * @param expectedScore The expected score (probabilistic estimate)
 * @param labelDistribution The distribution of predicted class probabilities
 * @param labelScores The score mapping for each label
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
  /**
   * Returns the predicted probability for a given label.
   *
   * @param label The label to look up
   * @param classValues The list of possible class labels
   * @return The probability assigned to the label
   */
  public double probabilityFor(
          final String label,
          final String[] classValues
  ) {
      for (int i = 0; i < classValues.length; i++) {
          if (classValues[i].equals(label)) {
              return labelDistribution[i];
          }
      }
      return 0.0;
  }

  /**
   * Formats class label probabilities as readable strings.
   *
   * @param classValues Array of class labels
   * @return Array of formatted probability strings
   */
  public String[] formatProbabilities(final String[] classValues) {
      String[] formatted = new String[classValues.length];
      for (int i = 0; i < classValues.length; i++) {
          formatted[i] = classValues[i]
                  + "="
                  + String.format("%.3f", labelDistribution[i]);
      }
      return formatted;
  }

  /**
   * Returns a debug summary string for this prediction.
   *
   * @param classValues Array of class labels
   * @return Summary string for debugging
   */
  public String debugSummary(final String[] classValues) {
      return "PredictionResult{"
              + "reviewId='" + reviewId + '\''
              + ", product='" + product + '\''
              + ", actual='" + actualLabel + '\''
              + ", predicted='" + predictedLabel + '\''
              + ", expectedScore=" + String.format("%.3f", expectedScore)
              + ", distribution="
              + Arrays.toString(formatProbabilities(classValues))
              + '}';
  }
}
