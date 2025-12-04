package com.nexus.sentiment;

import weka.core.Attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.Serializable;

/**
 * Maps sentiment labels to numeric scores.
 */
public final class ScoreMapper implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * Mapping from label to sentiment score.
   */
  private final Map<String, Double> scoreByLabel;

  /**
   * Private constructor.
   *
   * @param labelScoreMap Map of sentiment label to score.
   */
  private ScoreMapper(final Map<String, Double> labelScoreMap) {
      this.scoreByLabel = labelScoreMap;
  }

  /**
   * Creates a ScoreMapper from a Weka class attribute.
   *
   * @param classAttribute The class attribute containing label values.
   * @return ScoreMapper with label-to-score mappings.
   */
  public static ScoreMapper fromAttribute(final Attribute classAttribute) {
      if (classAttribute.numValues() == 0) {
        throw new IllegalArgumentException("No valid labels provided");
      }

      final double strongNeg = -1.0;
      final double weakNeg = -0.5;
      final double neutral = 0.0;
      final double weakPos = 0.5;
      final double strongPos = 1.0;

      final Map<String, Double> defaults = new HashMap<>();
      defaults.put("negative", strongNeg);
      defaults.put("neutral", neutral);
      defaults.put("positive", strongPos);
      defaults.put("very negative", strongNeg);
      defaults.put("somewhat negative", weakNeg);
      defaults.put("somewhat positive", weakPos);
      defaults.put("very positive", strongPos);

      final Map<String, Double> result = new HashMap<>();
      for (int i = 0; i < classAttribute.numValues(); i++) {
          final String label = classAttribute.value(i).toLowerCase();
          if (defaults.containsKey(label)) {
              result.put(label, defaults.get(label));
          }
      }

      if (result.size() == classAttribute.numValues()) {
          return new ScoreMapper(result);
      }

      final List<String> missing = new ArrayList<>();
      for (int i = 0; i < classAttribute.numValues(); i++) {
          final String label = classAttribute.value(i).toLowerCase();
          if (!result.containsKey(label)) {
              missing.add(label);
          }
      }

      if (!missing.isEmpty()) {
          final double min = strongNeg;
          final double max = strongPos;
          final double step = missing.size() == 1
                  ? 0
                  : (max - min) / (missing.size() - 1);
          for (int i = 0; i < missing.size(); i++) {
              result.put(missing.get(i), min + (step * i));
          }
      }

      return new ScoreMapper(result);
  }

  /**
   * Returns the numeric score for a given sentiment label.
   *
   * @param label Sentiment label.
   * @return Numeric score, or 0.0 if not found.
   */
  public double scoreFor(final String label) {
      return scoreByLabel.getOrDefault(
              label.toLowerCase(Locale.ROOT), 0.0);
  }

  /**
   * Returns a copy of the internal label-score mapping.
   *
   * @return Map of labels to scores.
   */
  public Map<String, Double> allScores() {
    return Map.copyOf(scoreByLabel);
  }
}
