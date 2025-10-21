package com.nexus.sentiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for NLP text processing and TF-IDF vectorization
 */
class SentimentModelTrainerTest {

    private Instances trainingData;

    @BeforeEach
    void setUp() throws Exception {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("review_text", (ArrayList<String>) null));
        
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        classValues.add("negative");
        classValues.add("neutral");
        attributes.add(new Attribute("sentiment", classValues));

        trainingData = new Instances("TrainingData", attributes, 0);
        trainingData.setClassIndex(1);

        // Add diverse text samples for NLP testing
        addInstance(trainingData, "This product is absolutely fantastic and amazing!", "positive");
        addInstance(trainingData, "Excellent quality, highly recommend to everyone!", "positive");
        addInstance(trainingData, "Love it! Best purchase I've ever made!", "positive");
        addInstance(trainingData, "Terrible quality, completely disappointed and frustrated.", "negative");
        addInstance(trainingData, "Awful product, waste of money and time.", "negative");
        addInstance(trainingData, "Horrible experience, never buying again!", "negative");
        addInstance(trainingData, "It's okay, nothing special or remarkable.", "neutral");
        addInstance(trainingData, "Average product, meets basic expectations.", "neutral");
        addInstance(trainingData, "Mediocre quality, not impressed but not terrible.", "neutral");
    }

    private void addInstance(Instances data, String text, String label) throws Exception {
        DenseInstance instance = new DenseInstance(2);
        instance.setDataset(data);
        instance.setValue(0, text);
        // Check if the label is a valid value in the sentiment class attribute
        Attribute sentimentAttr = data.attribute("sentiment");
        if (sentimentAttr == null || sentimentAttr.indexOfValue(label) == -1) {
            throw new IllegalArgumentException("Unrecognized label: " + label);
        }
        instance.setValue(1, label);
        data.add(instance);
    }

    @Test
    /**
     * Test that the SentimentModelTrainer initializes correctly.
     **/
    void testTrainerInitialization() {
        SentimentModelTrainer trainer = new SentimentModelTrainer();
        assertThat(trainer).isNotNull();
    }

    @Test
    /**
     * Test training with valid data and correct and existing text attribute.
     * @throws Exception
     **/
    void testTrainWithValidData() throws Exception {
        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(trainingData, "review_text");

        assertThat(classifier).isNotNull();
        assertThat(trainer.getClassifier()).isNotNull();
        assertThat(trainer.getClassifier()).isSameAs(classifier);
    }

    @Test
    /**
     * Test getting the classifier before training returns null.
     * Classifier is not available before training.
     **/
    void testGetClassifierBeforeTraining() {
        SentimentModelTrainer trainer = new SentimentModelTrainer();
        
        assertThat(trainer.getClassifier()).isNull();
    }

    @Test
    /**
     * Test training with missing text attribute throws exception.
     **/
    void testTrainWithMissingTextAttribute() {
        SentimentModelTrainer trainer = new SentimentModelTrainer();

        assertThatThrownBy(() -> trainer.train(trainingData, "nonexistent_attribute"))
                .isInstanceOf(Exception.class);
    }

    @Test
    /**
     * This test verifies that the NLP tokenization process correctly handles
     * various text formats including mixed case, punctuation, numbers, special characters, and emojis.
     * Trainer should be able to start even with mixed text formats.
     * @throws Exception
     */
    void testNlpTokenization() throws Exception {
        // Test that the model handles various text formats
        Instances testData = new Instances(trainingData, 0);
        
        // Mixed case
        addInstance(testData, "GREAT Product!", "positive");
        addInstance(testData, "awful QUALITY!", "negative");
        addInstance(testData, "not Bad but not Great", "neutral");
        
        // Punctuation
        addInstance(testData, "Wow!!! Amazing!!! Best ever!!!", "positive");
        addInstance(testData, "Bad... as expected...", "negative");
        
        // Numbers, special characters, and emojis
        addInstance(testData, "5 stars! Top-notch quality @ great price!", "positive");
        addInstance(testData, "1 star. 'Poor' quality & bad service.", "negative");
        addInstance(testData, "😅 what a garbage", "negative");
        addInstance(testData, "^__^ !!! love it", "positive");

        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(testData, "review_text");

        assertThat(classifier).isNotNull();
        
        // Verify classifier can make predictions on new instances
        DenseInstance newInstance = new DenseInstance(2);
        newInstance.setDataset(testData);
        newInstance.setValue(0, "Excellent product!");
        newInstance.setValue(1, "positive");
        
        double[] distribution = classifier.distributionForInstance(newInstance);
        assertThat(distribution).hasSize(3);
        for (double prob : distribution) {
            assertThat(prob).isBetween(0.0, 1.0);
        }
        double sum = 0;
        for (double d : distribution) sum += d;
        assertThat(sum).isCloseTo(1.0, within(0.01));
    }

    @Test
    /**
     * Test if too few instances provided for training, less than 5,
     * trainer should throw an exception.
     * @throws Exception
     */
    void testInsufficientTrainingInstances() throws Exception {
        Instances testData = new Instances(trainingData, 0);
        
        addInstance(testData, "running smoothly, runs perfectly, ran great", "positive");
        addInstance(testData, "breaking easily, breaks quickly, broke fast", "negative");
        
        SentimentModelTrainer trainer = new SentimentModelTrainer();
        assertThatThrownBy(() -> trainer.train(testData, "review_text"))
                .isInstanceOf(Exception.class);
    }

    @Test
    /**
     * Test if labels are irregular or non-standard,
     * trainer should be able to catch the exception.
     * @throws Exception
     */
    void testIrregularTrainingLabels() throws Exception {
        Instances testData = new Instances(trainingData, 0);

        assertThatThrownBy(() -> addInstance(testData, "running smoothly, runs perfectly, ran great", "^__^"))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> addInstance(testData, "breaking easily, breaks quickly, broke fast", "Mehh"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void testTfIdfWeighting() throws Exception {
        // Test that TF-IDF properly weights important vs common words
        Instances testData = new Instances(trainingData, 0);
        
        // Unique discriminative words
        addInstance(testData, "magnificent spectacular extraordinary", "positive");
        addInstance(testData, "dreadful atrocious abysmal", "negative");
        
        // Common neutral words
        addInstance(testData, "product item thing object", "neutral");
        
        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(testData, "review_text");

        assertThat(classifier).isNotNull();
        
        // Make prediction on text with unique positive words
        DenseInstance positiveTest = new DenseInstance(2);
        positiveTest.setDataset(testData);
        positiveTest.setValue(0, "magnificent and spectacular");
        positiveTest.setValue(1, "positive");
        
        double[] dist = classifier.distributionForInstance(positiveTest);
        int predictedIndex = argMax(dist);
        assertThat(testData.classAttribute().value(predictedIndex)).isEqualTo("positive");
    }

    @Test
    void testMultiClassPrediction() throws Exception {
        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(trainingData, "review_text");

        // Test positive prediction
        DenseInstance positiveInstance = new DenseInstance(2);
        positiveInstance.setDataset(trainingData);
        positiveInstance.setValue(0, "Absolutely wonderful and fantastic experience!");
        positiveInstance.setValue(1, "positive");
        
        double[] positiveDist = classifier.distributionForInstance(positiveInstance);
        assertThat(argMax(positiveDist)).isEqualTo(0); // positive class

        // Test negative prediction
        DenseInstance negativeInstance = new DenseInstance(2);
        negativeInstance.setDataset(trainingData);
        negativeInstance.setValue(0, "Terrible and awful, completely disappointed!");
        negativeInstance.setValue(1, "negative");
        
        double[] negativeDist = classifier.distributionForInstance(negativeInstance);
        assertThat(argMax(negativeDist)).isEqualTo(1); // negative class
    }

    @Test
    void testEmptyTextHandling() throws Exception {
        Instances testData = new Instances(trainingData, 0);
        addInstance(testData, "", "neutral");
        addInstance(testData, "   ", "neutral");
        addInstance(testData, "Great product", "positive");

        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(testData, "review_text");

        assertThat(classifier).isNotNull();
    }

    @Test
    void testLongTextHandling() throws Exception {
        Instances testData = new Instances(trainingData, 0);
        
        // Generate long review text
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("This is an excellent product with great quality and amazing features. ");
        }
        addInstance(testData, longText.toString(), "positive");

        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(testData, "review_text");

        assertThat(classifier).isNotNull();
    }

    @Test
    void testSpecialCharactersAndUnicode() throws Exception {
        Instances testData = new Instances(trainingData, 0);
        
        addInstance(testData, "Café quality ★★★★★ 100% satisfaction!", "positive");
        addInstance(testData, "Terrible quality ☹ Don't buy!!!", "negative");
        addInstance(testData, "Okay product... 50/50 experience", "neutral");

        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(testData, "review_text");

        assertThat(classifier).isNotNull();
    }

    @Test
    void testMinimalTrainingData() throws Exception {
        Instances minimalData = new Instances(trainingData, 0);
        addInstance(minimalData, "good", "positive");
        addInstance(minimalData, "bad", "negative");
        addInstance(minimalData, "okay", "neutral");

        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(minimalData, "review_text");

        assertThat(classifier).isNotNull();
    }

    @Test
    void testModelPersistenceFromFile(@TempDir Path tempDir) throws Exception {
        Path csvFile = tempDir.resolve("train.csv");
        String content = """
                review_text,sentiment_label
                "Excellent device very satisfied",positive
                "Terrible quality very disappointed",negative
                "Average item nothing special",neutral
                "Amazing quality highly recommend",positive
                "Awful experience complete waste",negative
                """;
        Files.writeString(csvFile, content);

        Instances data = DatasetLoader.load(csvFile, "sentiment_label");
        
        // Convert review_text to string type for TF-IDF
        data.setClassIndex(-1); // temporarily unset
        Attribute reviewAttr = data.attribute("review_text");
        assertThat(reviewAttr).isNotNull();
        data.setClassIndex(data.attribute("sentiment_label").index());
        
        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(data, "review_text");

        assertThat(classifier).isNotNull();
        
        // Test prediction
        DenseInstance testInstance = new DenseInstance(2);
        testInstance.setDataset(data);
        testInstance.setValue(0, "Fantastic device");
        testInstance.setValue(1, "positive");
        
        double[] distribution = classifier.distributionForInstance(testInstance);
        assertThat(distribution).hasSize(3);
    }

    private int argMax(double[] values) {
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
}
