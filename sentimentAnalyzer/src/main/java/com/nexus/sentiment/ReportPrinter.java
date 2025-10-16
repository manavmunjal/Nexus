package com.nexus.sentiment;

import weka.classifiers.Evaluation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prints evaluation summaries and statistical analyses to the console.
 */
public final class ReportPrinter {
    private ReportPrinter() {
    }

    public static void printEvaluation(Evaluation evaluation, String[] classValues) {
        System.out.println("==== Evaluation Metrics ====\n");
        System.out.printf("Accuracy: %.2f%%%n", evaluation.pctCorrect());
        for (int i = 0; i < classValues.length; i++) {
            System.out.printf("Class '%s' -> Precision: %.3f, Recall: %.3f, F1: %.3f%n",
                    classValues[i],
                    evaluation.precision(i),
                    evaluation.recall(i),
                    evaluation.fMeasure(i));
        }
        System.out.printf("Weighted AUC: %.3f%n", evaluation.weightedAreaUnderROC());
        System.out.println();
    }

    public static void printPredictions(List<PredictionResult> predictions, String[] classValues, int limit) {
        System.out.println("==== Sample Predictions ====\n");
        predictions.stream()
                .limit(limit)
                .forEach(result -> System.out.println(result.debugSummary(classValues)));
        System.out.println();
    }

    public static Map<String, SentimentStatistics.GroupStatistics> printGroupSummaries(
            String title,
            Map<String, List<PredictionResult>> grouped,
            String[] classValues
    ) {
    System.out.printf("==== %s ==== %n%n", title);
        Map<String, SentimentStatistics.GroupStatistics> summaries = new HashMap<>();
        List<String> orderedKeys = new ArrayList<>(grouped.keySet());
        orderedKeys.sort(Comparator.naturalOrder());
        for (String key : orderedKeys) {
            List<PredictionResult> items = grouped.get(key);
            SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize(key, items, classValues);
            summaries.put(key, stats);
            System.out.printf("%s -> total=%d, meanScore=%.3f, variance=%.3f, stdDev=%.3f, skewness=%.3f%n",
                    key,
                    stats.totalReviews(),
                    stats.meanScore(),
                    stats.variance(),
                    stats.standardDeviation(),
                    stats.skewness());
            System.out.printf("    Distribution: %s%n", formatMap(stats.labelProportions()));
        }
        System.out.println();
        return summaries;
    }

    public static void printKlDivergence(Map<String, SentimentStatistics.GroupStatistics> summaries, double epsilon) {
        if (summaries.size() < 2) {
            System.out.println("Insufficient groups for KL-divergence comparison.\n");
            return;
        }
    System.out.println("==== Symmetric KL Divergence ====\n");
        List<String> keys = new ArrayList<>(summaries.keySet());
        keys.sort(Comparator.naturalOrder());
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                SentimentStatistics.GroupStatistics left = summaries.get(keys.get(i));
                SentimentStatistics.GroupStatistics right = summaries.get(keys.get(j));
                Map<String, Double> p = DistributionUtils.smooth(left.labelProportions(), epsilon);
                Map<String, Double> q = DistributionUtils.smooth(right.labelProportions(), epsilon);
                double divergence = DistributionUtils.symmetricKlDivergence(p, q);
                System.out.printf("%s vs %s -> KL=%.4f%n", left.group(), right.group(), divergence);
            }
        }
        System.out.println();
    }

    private static String formatMap(Map<String, ?> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + String.format("%.3f", entry.getValue()))
                .collect(Collectors.joining(", "));
    }
}
