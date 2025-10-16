package com.nexus.sentiment;

import weka.core.Attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides numeric sentiment scores for categorical sentiment labels.
 */
public final class ScoreMapper {
    private final Map<String, Double> scoreByLabel;

    private ScoreMapper(Map<String, Double> scoreByLabel) {
        this.scoreByLabel = scoreByLabel;
    }

    public static ScoreMapper fromAttribute(Attribute classAttribute) {
        Map<String, Double> defaults = new HashMap<>();
        // 3-class common mapping
        defaults.put("negative", -1.0);
        defaults.put("neutral", 0.0);
        defaults.put("positive", 1.0);
        // 5-degree mapping
        defaults.put("very negative", -1.0);
        defaults.put("somewhat negative", -0.5);
        defaults.put("somewhat positive", 0.5);
        defaults.put("very positive", 1.0);
        Map<String, Double> result = new HashMap<>();
        for (int i = 0; i < classAttribute.numValues(); i++) {
            String label = classAttribute.value(i);
            if (defaults.containsKey(label.toLowerCase())) {
                result.put(label, defaults.get(label.toLowerCase()));
            }
        }
        if (result.size() == classAttribute.numValues()) {
            return new ScoreMapper(result);
        }
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < classAttribute.numValues(); i++) {
            String label = classAttribute.value(i);
            if (!result.containsKey(label)) {
                missing.add(label);
            }
        }
        if (!missing.isEmpty()) {
            double min = -1.0;
            double max = 1.0;
            double step = missing.size() == 1 ? 0 : (max - min) / (missing.size() - 1);
            for (int i = 0; i < missing.size(); i++) {
                result.put(missing.get(i), min + (step * i));
            }
        }
        return new ScoreMapper(result);
    }

    public double scoreFor(String label) {
        return scoreByLabel.getOrDefault(label, 0.0);
    }

    public Map<String, Double> allScores() {
        return Map.copyOf(scoreByLabel);
    }
}
