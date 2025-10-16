package com.nexus.sentiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SentimentPredictorTest {

    private FilteredClassifier classifier;
    private Instances testData;
    private ScoreMapper scoreMapper;

    @BeforeEach
    void setUp() throws Exception {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("review_text", (ArrayList<String>) null));

        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("very positive");
        classValues.add("somewhat positive");
        classValues.add("neutral");
        classValues.add("somewhat negative");
        classValues.add("very negative");
        attributes.add(new Attribute("sentiment", classValues));

        testData = new Instances("TestData", attributes, 0);
        testData.setClassIndex(1);

        // Add training instances
        addInstance(testData, "This is excellent and amazing!", "very positive");
        addInstance(testData, "Terrible and awful quality!", "very negative");
        addInstance(testData, "Average experience, nothing special.", "neutral");
        addInstance(testData, "Great quality, highly recommend!", "very positive");
        addInstance(testData, "Poor quality, very disappointed.", "very negative");
        addInstance(testData, "Good product, works fine.", "somewhat positive");
        addInstance(testData, "Not great, could be better.", "somewhat negative");

        // Train a simple classifier
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        classifier = trainer.train(testData);

        scoreMapper = ScoreMapper.fromAttribute(testData.classAttribute());
    }

    private void addInstance(Instances data, String text, String label) {
        DenseInstance instance = new DenseInstance(2);
        instance.setDataset(data);
        instance.setValue(0, text);
        instance.setValue(1, label);
        data.add(instance);
    }

    @Test
    void testPredictReturnsResults() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        assertThat(results).isNotEmpty();
        assertThat(results).hasSize(testData.numInstances());
    }

    @Test
    void testPredictionContainsAllFields() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        PredictionResult result = results.get(0);
        assertThat(result.reviewId()).isEmpty();
        assertThat(result.company()).isEmpty();
        assertThat(result.product()).isEmpty();
        assertThat(result.actualLabel()).isNotBlank();
        assertThat(result.predictedLabel()).isNotBlank();
        assertThat(result.labelDistribution()).hasSize(5);
        assertThat(result.labelScores()).isNotEmpty();
    }

    @Test
    void testExpectedScoreInRange() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        for (PredictionResult result : results) {
            assertThat(result.expectedScore()).isBetween(-1.0, 1.0);
        }
    }

    @Test
    void testDistributionSumsToOne() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        for (PredictionResult result : results) {
            double sum = 0;
            for (double prob : result.labelDistribution()) {
                sum += prob;
            }
            assertThat(sum).isCloseTo(1.0, within(0.001));
        }
    }

    @Test
    void testDistributionProbabilitiesValid() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        for (PredictionResult result : results) {
            for (double prob : result.labelDistribution()) {
                assertThat(prob).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    void testProbabilityForMethod() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"very positive", "somewhat positive", "neutral", "somewhat negative", "very negative"};

        PredictionResult result = results.get(0);
        double veryPosProb = result.probabilityFor("very positive", classValues);
        double somewhatPosProb = result.probabilityFor("somewhat positive", classValues);
        double neuProb = result.probabilityFor("neutral", classValues);
        double somewhatNegProb = result.probabilityFor("somewhat negative", classValues);
        double veryNegProb = result.probabilityFor("very negative", classValues);

        assertThat(veryPosProb).isBetween(0.0, 1.0);
        assertThat(somewhatPosProb).isBetween(0.0, 1.0);
        assertThat(neuProb).isBetween(0.0, 1.0);
        assertThat(somewhatNegProb).isBetween(0.0, 1.0);
        assertThat(veryNegProb).isBetween(0.0, 1.0);
    }

    @Test
    void testProbabilityForUnknownLabel() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"very positive", "somewhat positive", "neutral", "somewhat negative", "very negative"};

        PredictionResult result = results.get(0);
        double unknownProb = result.probabilityFor("unknown_label", classValues);

        assertThat(unknownProb).isEqualTo(0.0);
    }

    @Test
    void testFormatProbabilities() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"very positive", "somewhat positive", "neutral", "somewhat negative", "very negative"};

        PredictionResult result = results.get(0);
        String[] formatted = result.formatProbabilities(classValues);

        assertThat(formatted).hasSize(5);
        for (String format : formatted) {
            assertThat(format).contains("=");
        }
    }

    @Test
    void testDebugSummary() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"very positive", "somewhat positive", "neutral", "somewhat negative", "very negative"};

        PredictionResult result = results.get(0);
        String summary = result.debugSummary(classValues);

        assertThat(summary).contains("PredictionResult");
        assertThat(summary).contains("reviewId");
        assertThat(summary).contains("product");
        assertThat(summary).contains("actual");
        assertThat(summary).contains("predicted");
    }

    @Test
    void testMissingAttributes() throws Exception {
        // Create instances without review_id, company, product
        ArrayList<Attribute> minimalAttributes = new ArrayList<>();
        minimalAttributes.add(new Attribute("review_text", (ArrayList<String>) null));

        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("very positive");
        classValues.add("somewhat positive");
        classValues.add("neutral");
        classValues.add("somewhat negative");
        classValues.add("very negative");
        minimalAttributes.add(new Attribute("sentiment", classValues));

        Instances minimalData = new Instances("MinimalData", minimalAttributes, 0);
        minimalData.setClassIndex(1);

        DenseInstance instance = new DenseInstance(2);
        instance.setDataset(minimalData);
        instance.setValue(0, "Great product!");
        instance.setValue(1, "very positive");
        minimalData.add(instance);

        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier minimalClassifier = trainer.train(minimalData);

        List<PredictionResult> results = SentimentPredictor.predict(minimalClassifier, minimalData, scoreMapper);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).reviewId()).isEmpty();
        assertThat(results.get(0).company()).isEmpty();
        assertThat(results.get(0).product()).isEmpty();
    }

    @Test
    void testPredictedLabelMatchesHighestProbability() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"very positive", "somewhat positive", "neutral", "somewhat negative", "very negative"};

        for (PredictionResult result : results) {
            double maxProb = -1;
            String maxLabel = "";
            
            for (int i = 0; i < result.labelDistribution().length; i++) {
                if (result.labelDistribution()[i] > maxProb) {
                    maxProb = result.labelDistribution()[i];
                    maxLabel = classValues[i];
                }
            }
            
            assertThat(result.predictedLabel()).isEqualTo(maxLabel);
        }
    }

    @Test
    void testExpectedScoreCalculation() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        for (PredictionResult result : results) {
            // Manually calculate expected score
            double[] dist = result.labelDistribution();
            Map<String, Double> scores = result.labelScores();
            String[] classValues = new String[]{"very positive", "somewhat positive", "neutral", "somewhat negative", "very negative"};
            
            double expectedScore = 0.0;
            for (int i = 0; i < dist.length; i++) {
                expectedScore += dist[i] * scores.get(classValues[i]);
            }
            
            assertThat(result.expectedScore()).isCloseTo(expectedScore, within(0.001));
        }
    }
}
