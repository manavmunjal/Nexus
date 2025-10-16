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
    }

    public static List<PredictionResult> predict(FilteredClassifier classifier, Instances instances, ScoreMapper scoreMapper) throws Exception {
        List<PredictionResult> results = new ArrayList<>();
        Attribute classAttribute = instances.classAttribute();
        String[] classValues = new String[classAttribute.numValues()];
        for (int i = 0; i < classAttribute.numValues(); i++) {
            classValues[i] = classAttribute.value(i);
        }

        for (Instance instance : instances) {
            double[] distribution = classifier.distributionForInstance(instance);
            int predictedIndex = argMax(distribution);
            String predictedLabel = classAttribute.value(predictedIndex);

            String actualLabel = instance.classIsMissing() ? "unknown" : instance.stringValue(classAttribute);
            double expectedScore = 0.0;
            for (int i = 0; i < distribution.length; i++) {
                expectedScore += distribution[i] * scoreMapper.scoreFor(classAttribute.value(i));
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

    private static int argMax(double[] values) {
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

    private static String readString(Instance instance, String attributeName) {
        Attribute attr = instance.dataset().attribute(attributeName);
        if (attr == null) {
            return "";
        }
        if (attr.isString()) {
            return instance.stringValue(attr);
        }
        if (attr.isNominal()) {
            return instance.stringValue(attr);
        }
        return Double.toString(instance.value(attr));
    }
}
