package com.nexus.sentiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class SentimentStatisticsTest {

    private List<PredictionResult> predictions;
    private String[] classValues;

    @BeforeEach
    void setUp() {
        classValues = new String[]{"positive", "negative", "neutral"};
        predictions = new ArrayList<>();

        Map<String, Double> labelScores = Map.of(
                "positive", 1.0,
                "negative", -1.0,
                "neutral", 0.0
        );

        predictions.add(new PredictionResult("1", "CompanyA", "ProductX", "positive", "positive", 1.0, new double[]{0.9, 0.05, 0.05}, labelScores));
        predictions.add(new PredictionResult("2", "CompanyA", "ProductX", "positive", "positive", 0.8, new double[]{0.8, 0.1, 0.1}, labelScores));
        predictions.add(new PredictionResult("3", "CompanyA", "ProductX", "negative", "negative", -0.9, new double[]{0.05, 0.9, 0.05}, labelScores));
        predictions.add(new PredictionResult("4", "CompanyA", "ProductX", "neutral", "neutral", 0.0, new double[]{0.3, 0.3, 0.4}, labelScores));
    }

    @Test
    void testSummarizeBasicStatistics() {
        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("TestGroup", predictions, classValues);

        assertThat(stats.group()).isEqualTo("TestGroup");
        assertThat(stats.totalReviews()).isEqualTo(4);
        assertThat(stats.labelCounts().get("positive")).isEqualTo(2);
        assertThat(stats.labelCounts().get("negative")).isEqualTo(1);
        assertThat(stats.labelCounts().get("neutral")).isEqualTo(1);
    }

    @Test
    void testProportionsCalculation() {
        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("TestGroup", predictions, classValues);

        assertThat(stats.labelProportions().get("positive")).isCloseTo(0.5, within(0.01));
        assertThat(stats.labelProportions().get("negative")).isCloseTo(0.25, within(0.01));
        assertThat(stats.labelProportions().get("neutral")).isCloseTo(0.25, within(0.01));
    }

    @Test
    void testMeanScoreCalculation() {
        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("TestGroup", predictions, classValues);

        // Mean of [1.0, 0.8, -0.9, 0.0] = 0.225
        assertThat(stats.meanScore()).isCloseTo(0.225, within(0.01));
    }

    @Test
    void testVarianceAndStdDevCalculation() {
        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("TestGroup", predictions, classValues);

        assertThat(stats.variance()).isGreaterThan(0.0);
        assertThat(stats.standardDeviation()).isGreaterThan(0.0);
        
        // Standard deviation should be square root of variance
        assertThat(Math.pow(stats.standardDeviation(), 2)).isCloseTo(stats.variance(), within(0.001));
    }

    @Test
    void testSkewnessCalculation() {
        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("TestGroup", predictions, classValues);

        // Skewness should be a finite number
        assertThat(stats.skewness()).isNotNaN();
        assertThat(stats.skewness()).isFinite();
    }

    @Test
    void testEmptyPredictions() {
        List<PredictionResult> empty = new ArrayList<>();
        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("Empty", empty, classValues);

        assertThat(stats.totalReviews()).isEqualTo(0);
        assertThat(stats.meanScore()).isEqualTo(0.0);
        assertThat(stats.variance()).isEqualTo(0.0);
        assertThat(stats.standardDeviation()).isEqualTo(0.0);
        assertThat(stats.skewness()).isEqualTo(0.0);
    }

    @Test
    void testSinglePrediction() {
        List<PredictionResult> single = List.of(predictions.get(0));
        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("Single", single, classValues);

        assertThat(stats.totalReviews()).isEqualTo(1);
        assertThat(stats.meanScore()).isEqualTo(1.0);
        assertThat(stats.labelProportions().get("positive")).isEqualTo(1.0);
    }

    @Test
    void testAllPositivePredictions() {
        Map<String, Double> labelScores = Map.of(
                "positive", 1.0,
                "negative", -1.0,
                "neutral", 0.0
        );

        List<PredictionResult> allPositive = List.of(
                new PredictionResult("1", "Co", "Prod", "positive", "positive", 1.0, new double[]{1.0, 0, 0}, labelScores),
                new PredictionResult("2", "Co", "Prod", "positive", "positive", 1.0, new double[]{1.0, 0, 0}, labelScores),
                new PredictionResult("3", "Co", "Prod", "positive", "positive", 1.0, new double[]{1.0, 0, 0}, labelScores)
        );

        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("AllPositive", allPositive, classValues);

        assertThat(stats.meanScore()).isEqualTo(1.0);
        assertThat(stats.labelProportions().get("positive")).isEqualTo(1.0);
        assertThat(stats.labelProportions().get("negative")).isEqualTo(0.0);
        assertThat(stats.variance()).isEqualTo(0.0);
        assertThat(stats.skewness()).isEqualTo(0.0); // No skew when all values are identical
    }

    @Test
    void testPositiveSkewness() {
        // Create distribution skewed to the right (positive skew)
        Map<String, Double> labelScores = Map.of(
                "positive", 1.0,
                "negative", -1.0,
                "neutral", 0.0
        );

        List<PredictionResult> positiveSkew = List.of(
                new PredictionResult("1", "Co", "Prod", "negative", "negative", -1.0, new double[]{0, 1.0, 0}, labelScores),
                new PredictionResult("2", "Co", "Prod", "negative", "negative", -1.0, new double[]{0, 1.0, 0}, labelScores),
                new PredictionResult("3", "Co", "Prod", "negative", "negative", -0.5, new double[]{0, 1.0, 0}, labelScores),
                new PredictionResult("4", "Co", "Prod", "positive", "positive", 1.0, new double[]{1.0, 0, 0}, labelScores)
        );

        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("PositiveSkew", positiveSkew, classValues);

        // Skewness should be positive (tail extends to the right)
        assertThat(stats.skewness()).isGreaterThan(0.0);
    }

    @Test
    void testImmutabilityOfStatistics() {
        SentimentStatistics.GroupStatistics stats = SentimentStatistics.summarize("Test", predictions, classValues);

        assertThatThrownBy(() -> stats.labelCounts().put("new_label", 100L))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> stats.labelProportions().put("new_label", 0.5))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testMultipleGroups() {
        Map<String, Double> labelScores = Map.of(
                "positive", 1.0,
                "negative", -1.0,
                "neutral", 0.0
        );

        List<PredictionResult> group1 = List.of(
                new PredictionResult("1", "CompanyA", "Prod1", "positive", "positive", 1.0, new double[]{1.0, 0, 0}, labelScores),
                new PredictionResult("2", "CompanyA", "Prod1", "positive", "positive", 0.8, new double[]{0.8, 0.1, 0.1}, labelScores)
        );

        List<PredictionResult> group2 = List.of(
                new PredictionResult("3", "CompanyB", "Prod2", "negative", "negative", -1.0, new double[]{0, 1.0, 0}, labelScores),
                new PredictionResult("4", "CompanyB", "Prod2", "negative", "negative", -0.9, new double[]{0, 0.9, 0.1}, labelScores)
        );

        SentimentStatistics.GroupStatistics stats1 = SentimentStatistics.summarize("Group1", group1, classValues);
        SentimentStatistics.GroupStatistics stats2 = SentimentStatistics.summarize("Group2", group2, classValues);

        assertThat(stats1.meanScore()).isGreaterThan(stats2.meanScore());
        assertThat(stats1.labelProportions().get("positive")).isGreaterThan(stats2.labelProportions().get("positive"));
    }
}
