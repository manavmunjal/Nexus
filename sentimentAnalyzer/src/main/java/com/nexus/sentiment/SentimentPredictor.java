package com.nexus.sentiment;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates per-review predictions using a trained classifier.
 */
public final class SentimentPredictor {

  private SentimentPredictor() {
      // Prevent instantiation
  }

  /**
   * Predict sentiment labels for each instance using the given classifier.
   *
   * @param classifier The trained Weka classifier
   * @param instances The dataset instances to predict on
   * @param scoreMapper Mapper to convert labels to scores
   * @return List of prediction results
   * @throws Exception if prediction fails
   */
  public static List<PredictionResult> predict(
          final FilteredClassifier classifier,
          final Instances instances,
          final ScoreMapper scoreMapper) throws Exception {

      final List<PredictionResult> results = new ArrayList<>();
      final Attribute classAttribute = instances.classAttribute();
      final String[] classValues = new String[classAttribute.numValues()];

      for (int i = 0; i < classAttribute.numValues(); i++) {
          classValues[i] = classAttribute.value(i);
      }

      for (final Instance instance : instances) {
          final double[] distribution =
                  classifier.distributionForInstance(instance);
          final int predictedIndex = argMax(distribution);
          final String predictedLabel =
                  classAttribute.value(predictedIndex);

          final String actualLabel = instance.classIsMissing()
                  ? "unknown"
                  : instance.stringValue(classAttribute);

          double expectedScore = 0.0;
          for (int i = 0; i < distribution.length; i++) {
              expectedScore += distribution[i]
                      * scoreMapper.scoreFor(classAttribute.value(i));
          }

          results.add(new PredictionResult(
                  readString(instance, "review_id"),
                  readString(instance, "company"),
                  readString(instance, "product"),
                  actualLabel,
                  predictedLabel,
                  expectedScore,
                  distribution,
                  scoreMapper.allScores()
          ));
      }

      return results;
  }

  /**
   * Returns the index of the maximum value in the array.
   *
   * @param values Array of double values
   * @return Index of the maximum value
   */
  private static int argMax(final double[] values) {
      int maxIndex = 0;
      double maxValue = Double.NEGATIVE_INFINITY;
      for (int i = 0; i < values.length; i++) {
          if (values[i] > maxValue) {
              maxValue = values[i];
              maxIndex = i;
          }
      }
      return maxIndex;
  }

  /**
   * Reads the string value of an attribute from an instance.
   *
   * @param instance The instance
   * @param attributeName The attribute name
   * @return The string value, or empty string if attribute not found
   */
  private static String readString(
          final Instance instance,
          final String attributeName) {

      final Attribute attr = instance.dataset().attribute(attributeName);
      if (attr == null) {
          return "";
      }
      if (attr.isString() || attr.isNominal()) {
          return instance.stringValue(attr);
      }
      return Double.toString(instance.value(attr));
  }
}
