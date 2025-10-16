package com.nexus.sentiment;

import org.junit.jupiter.api.Test;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instances;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end tests that validate the full analysis pipeline
 * matches the service goals: proportions, variance/stddev, skewness,
 * and symmetric KL-divergence comparisons across groups.
 */
class EndToEndAnalysisTest {

    @Test
    void endToEndPipelineCoversServiceGoals() throws Exception {
        Path csv = resolveResource("/data/test_reviews.csv");

        // Load dataset and prepare labels
        Instances data = DatasetLoader.load(csv, "sentiment_label");
        Attribute classAttr = data.classAttribute();
        String[] classValues = new String[classAttr.numValues()];
        for (int i = 0; i < classAttr.numValues(); i++) {
            classValues[i] = classAttr.value(i);
        }

        // Train/test split and model training
        DataSplitter.Split split = DataSplitter.split(data, 0.75, 123);
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(split.train());
        ScoreMapper scoreMapper = ScoreMapper.fromAttribute(classAttr);

        // Predictions
        List<PredictionResult> predictions = SentimentPredictor.predict(classifier, split.test(), scoreMapper);
        assertThat(predictions).isNotEmpty();
        assertThat(predictions).hasSize(split.test().numInstances());

        // Group by product and company
        Map<String, List<PredictionResult>> byProduct = groupBy(predictions, PredictionResult::product, "UNKNOWN_PRODUCT");
        Map<String, List<PredictionResult>> byCompany = groupBy(predictions, PredictionResult::company, "UNKNOWN_COMPANY");

        // Summaries: proportions (sum to 1), variance/stddev non-negative, skewness finite
        Map<String, SentimentStatistics.GroupStatistics> productSummaries = summarize(byProduct, classValues);
        Map<String, SentimentStatistics.GroupStatistics> companySummaries = summarize(byCompany, classValues);

        for (SentimentStatistics.GroupStatistics stats : productSummaries.values()) {
            validateStats(stats);
        }
        for (SentimentStatistics.GroupStatistics stats : companySummaries.values()) {
            validateStats(stats);
        }

        // Symmetric KL-divergence comparisons
        assertSymmetricKlNonNegative(productSummaries, 1e-6);
        assertSymmetricKlNonNegative(companySummaries, 1e-6);
    }

    private static Path resolveResource(String resource) throws URISyntaxException {
        var url = Objects.requireNonNull(EndToEndAnalysisTest.class.getResource(resource),
                "Missing test resource: " + resource);
        return Paths.get(url.toURI());
    }

    private static Map<String, List<PredictionResult>> groupBy(
            List<PredictionResult> predictions,
            java.util.function.Function<PredictionResult, String> classifier,
            String fallback
    ) {
        Map<String, List<PredictionResult>> grouped = new HashMap<>();
        for (PredictionResult result : predictions) {
            String key = classifier.apply(result);
            if (key == null || key.isBlank()) {
                key = fallback;
            }
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(result);
        }
        return grouped;
    }

    private static Map<String, SentimentStatistics.GroupStatistics> summarize(
            Map<String, List<PredictionResult>> groups,
            String[] classValues
    ) {
        Map<String, SentimentStatistics.GroupStatistics> summaries = new HashMap<>();
        for (Map.Entry<String, List<PredictionResult>> e : groups.entrySet()) {
            summaries.put(e.getKey(), SentimentStatistics.summarize(e.getKey(), e.getValue(), classValues));
        }
        return summaries;
    }

    private static void validateStats(SentimentStatistics.GroupStatistics stats) {
        assertThat(stats.totalReviews()).isGreaterThan(0);
        assertThat(stats.variance()).isGreaterThanOrEqualTo(0.0);
        assertThat(stats.standardDeviation()).isGreaterThanOrEqualTo(0.0);
        assertThat(stats.skewness()).isFinite();

        double sum = stats.labelProportions().values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0, within(1e-6));

        // Keys in proportions should match counts
        assertThat(stats.labelProportions().keySet()).isEqualTo(stats.labelCounts().keySet());
    }

    private static void assertSymmetricKlNonNegative(
            Map<String, SentimentStatistics.GroupStatistics> summaries,
            double epsilon
    ) {
        if (summaries.size() < 2) {
            return; // nothing to compare
        }
        List<String> keys = new ArrayList<>(summaries.keySet());
        Collections.sort(keys);
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                var left = summaries.get(keys.get(i));
                var right = summaries.get(keys.get(j));
                Map<String, Double> p = DistributionUtils.smooth(left.labelProportions(), epsilon);
                Map<String, Double> q = DistributionUtils.smooth(right.labelProportions(), epsilon);
                double kl = DistributionUtils.symmetricKlDivergence(p, q);
                double klReverse = DistributionUtils.symmetricKlDivergence(q, p);
                assertThat(kl).isGreaterThanOrEqualTo(0.0);
                assertThat(kl).isCloseTo(klReverse, within(1e-12));
            }
        }
    }
}
