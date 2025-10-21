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
        attributes.add(new Attribute("review_id", (ArrayList<String>) null));
        attributes.add(new Attribute("company", (ArrayList<String>) null));
        attributes.add(new Attribute("product", (ArrayList<String>) null));
        attributes.add(new Attribute("review_text", (ArrayList<String>) null));

        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        classValues.add("negative");
        classValues.add("neutral");
        attributes.add(new Attribute("sentiment", classValues));

        testData = new Instances("TestData", attributes, 0);
        testData.setClassIndex(4);

        // Add training instances
        addInstance(testData, "1", "CompanyA", "ItemX", "This is excellent and amazing!", "positive");
        addInstance(testData, "2", "CompanyA", "ItemX", "Terrible and awful quality!", "negative");
        addInstance(testData, "3", "CompanyA", "ItemY", "Average experience, nothing special.", "neutral");
        addInstance(testData, "4", "CompanyB", "ItemZ", "Great quality, highly recommend!", "positive");
        addInstance(testData, "5", "CompanyB", "ItemZ", "Poor quality, very disappointed.", "negative");

        // Train a simple classifier
        SentimentModelTrainer trainer = new SentimentModelTrainer();
        classifier = trainer.train(testData, "review_text");

        scoreMapper = ScoreMapper.fromAttribute(testData.classAttribute());
    }

    private void addInstance(Instances data, String id, String company, String product, String text, String label) {
        DenseInstance instance = new DenseInstance(5);
        instance.setDataset(data);
        instance.setValue(0, id);
        instance.setValue(1, company);
        instance.setValue(2, product);
        instance.setValue(3, text);
        instance.setValue(4, label);
        data.add(instance);
    }

    @Test
    /**
     * Test that predictions are returned for all instances.
     * @throws Exception
     */
    void testPredictReturnsResults() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        assertThat(results).isNotEmpty();
        assertThat(results).hasSize(testData.numInstances());
    }

    @Test
    /**
     * Test that each PredictionResult contains all expected fields.
     * @throws Exception
     */
    void testPredictionContainsAllFields() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        PredictionResult result = results.get(0);
        assertThat(result.reviewId()).isNotBlank();
        assertThat(result.company()).isNotBlank();
        assertThat(result.product()).isNotBlank();
        assertThat(result.actualLabel()).isNotBlank();
        assertThat(result.predictedLabel()).isNotBlank();
        assertThat(result.labelDistribution()).hasSize(3);
        assertThat(result.labelScores()).isNotEmpty();
    }

    @Test
    /**
     * Test that expected scores are within valid range.
     * @throws Exception
     */
    void testExpectedScoreInRange() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        for (PredictionResult result : results) {
            assertThat(result.expectedScore()).isBetween(-1.0, 1.0);
        }
    }

    @Test
    /**
     * Test that label distributions sum to 1.0.
     * @throws Exception
     */
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
    /**
     * Test that label distribution probabilities are between 0.0 and 1.0.
     * @throws Exception
     */
    void testDistributionProbabilitiesValid() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        for (PredictionResult result : results) {
            for (double prob : result.labelDistribution()) {
                assertThat(prob).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    /**
     * Test that probabilityFor method returns correct probabilities.
     * @throws Exception
     */
    void testProbabilityForMethod() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"positive", "negative", "neutral"};

        PredictionResult result = results.get(0);
        double posProb = result.probabilityFor("positive", classValues);
        double negProb = result.probabilityFor("negative", classValues);
        double neuProb = result.probabilityFor("neutral", classValues);

        assertThat(posProb).isBetween(0.0, 1.0);
        assertThat(negProb).isBetween(0.0, 1.0);
        assertThat(neuProb).isBetween(0.0, 1.0);
    }

    @Test
    /**
     * Test that probabilityFor method returns 0.0 for unknown label.
     * @throws Exception
     */
    void testProbabilityForUnknownLabel() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"positive", "negative", "neutral"};

        PredictionResult result = results.get(0);
        double unknownProb = result.probabilityFor("unknown_label", classValues);

        assertThat(unknownProb).isEqualTo(0.0);
    }

    @Test
    /**
     * Test formatProbabilities method.
     * @throws Exception
     */
    void testFormatProbabilities() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"positive", "negative", "neutral"};

        PredictionResult result = results.get(0);
        String[] formatted = result.formatProbabilities(classValues);

        assertThat(formatted).hasSize(3);
        for (String format : formatted) {
            assertThat(format).contains("=");
        }
    }

    @Test
    /**
     * Test debug summary output.
     * @throws Exception
     */
    void testDebugSummary() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"positive", "negative", "neutral"};

        PredictionResult result = results.get(0);
        String summary = result.debugSummary(classValues);

        assertThat(summary).contains("PredictionResult");
        assertThat(summary).contains("reviewId");
        assertThat(summary).contains("product");
        assertThat(summary).contains("actual");
        assertThat(summary).contains("predicted");
    }

    @Test
    /**
     * Test that missing optional attributes are handled gracefully.
     * @throws Exception
     */
    void testMissingAttributes() throws Exception {
        // Create instances without review_id, company, product
        ArrayList<Attribute> minimalAttributes = new ArrayList<>();
        minimalAttributes.add(new Attribute("review_text", (ArrayList<String>) null));

        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        classValues.add("negative");
        classValues.add("neutral");
        minimalAttributes.add(new Attribute("sentiment", classValues));

        Instances minimalData = new Instances("MinimalData", minimalAttributes, 0);
        minimalData.setClassIndex(1);

        // Add minimum 5 instances required for training
        DenseInstance instance1 = new DenseInstance(2);
        instance1.setDataset(minimalData);
        instance1.setValue(0, "Great product!");
        instance1.setValue(1, "positive");
        minimalData.add(instance1);

        DenseInstance instance2 = new DenseInstance(2);
        instance2.setDataset(minimalData);
        instance2.setValue(0, "Terrible quality!");
        instance2.setValue(1, "negative");
        minimalData.add(instance2);

        DenseInstance instance3 = new DenseInstance(2);
        instance3.setDataset(minimalData);
        instance3.setValue(0, "It's okay");
        instance3.setValue(1, "neutral");
        minimalData.add(instance3);

        DenseInstance instance4 = new DenseInstance(2);
        instance4.setDataset(minimalData);
        instance4.setValue(0, "Excellent product!");
        instance4.setValue(1, "positive");
        minimalData.add(instance4);

        DenseInstance instance5 = new DenseInstance(2);
        instance5.setDataset(minimalData);
        instance5.setValue(0, "Not good!");
        instance5.setValue(1, "negative");
        minimalData.add(instance5);

        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier minimalClassifier = trainer.train(minimalData, "review_text");

        List<PredictionResult> results = SentimentPredictor.predict(minimalClassifier, minimalData, scoreMapper);

        assertThat(results).hasSize(5);
        // All results should have empty optional fields (review_id, company, product)
        for (PredictionResult result : results) {
            assertThat(result.reviewId()).isEmpty();
            assertThat(result.company()).isEmpty();
            assertThat(result.product()).isEmpty();
        }
    }

    @Test
    /**
     * Test that predicted label matches highest probability in distribution.
     * @throws Exception
     */
    void testPredictedLabelMatchesHighestProbability() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);
        String[] classValues = new String[]{"positive", "negative", "neutral"};

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
    /**
     * Test expected score calculation correctness.
     * @throws Exception
     */
    void testExpectedScoreCalculation() throws Exception {
        List<PredictionResult> results = SentimentPredictor.predict(classifier, testData, scoreMapper);

        for (PredictionResult result : results) {
            // Manually calculate expected score
            double[] dist = result.labelDistribution();
            Map<String, Double> scores = result.labelScores();
            String[] classValues = new String[]{"positive", "negative", "neutral"};
            
            double expectedScore = 0.0;
            for (int i = 0; i < dist.length; i++) {
                expectedScore += dist[i] * scores.get(classValues[i]);
            }
            
            assertThat(result.expectedScore()).isCloseTo(expectedScore, within(0.001));
        }
    }
}
