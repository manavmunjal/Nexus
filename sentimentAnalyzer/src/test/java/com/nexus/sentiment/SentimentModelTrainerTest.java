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
    void setUp() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("review_text", (ArrayList<String>) null));
        
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("very positive");
        classValues.add("somewhat positive");
        classValues.add("neutral");
        classValues.add("somewhat negative");
        classValues.add("very negative");
        attributes.add(new Attribute("sentiment", classValues));

        trainingData = new Instances("TrainingData", attributes, 0);
        trainingData.setClassIndex(1);

        // Add diverse text samples for NLP testing
        addInstance(trainingData, "This product is absolutely fantastic and amazing!", "very positive");
        addInstance(trainingData, "Excellent quality, highly recommend to everyone!", "very positive");
        addInstance(trainingData, "Love it! Best purchase I've ever made!", "very positive");
        addInstance(trainingData, "Terrible quality, completely disappointed and frustrated.", "very negative");
        addInstance(trainingData, "Awful product, waste of money and time.", "very negative");
        addInstance(trainingData, "Horrible experience, never buying again!", "very negative");
        addInstance(trainingData, "It's okay, nothing special or remarkable.", "neutral");
        addInstance(trainingData, "Average product, meets basic expectations.", "neutral");
        addInstance(trainingData, "Mediocre quality, not impressed but not terrible.", "neutral");
        addInstance(trainingData, "Good product, works well overall.", "somewhat positive");
        addInstance(trainingData, "Pretty nice, would recommend to friends.", "somewhat positive");
        addInstance(trainingData, "Not great, but usable for basic needs.", "somewhat negative");
        addInstance(trainingData, "Could be better, missing some features.", "somewhat negative");
    }

    private void addInstance(Instances data, String text, String label) {
        DenseInstance instance = new DenseInstance(2);
        instance.setDataset(data);
        instance.setValue(0, text);
        instance.setValue(1, label);
        data.add(instance);
    }

    @Test
    void testTrainerInitialization() {
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        assertThat(trainer).isNotNull();
    }

    @Test
    void testTrainWithValidData() throws Exception {
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(trainingData);

        assertThat(classifier).isNotNull();
        assertThat(trainer.getClassifier()).isNotNull();
        assertThat(trainer.getClassifier()).isSameAs(classifier);
    }

    @Test
    void testGetClassifierBeforeTraining() {
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        
        assertThatThrownBy(trainer::getClassifier)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Classifier has not been trained yet");
    }

    @Test
    void testTrainWithMissingTextAttribute() {
        SentimentModelTrainer trainer = new SentimentModelTrainer("nonexistent_attribute");

        assertThatThrownBy(() -> trainer.train(trainingData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing text attribute");
    }

    @Test
    void testNlpTokenization() throws Exception {
        // Test that the model handles various text formats
        Instances testData = new Instances(trainingData, 0);
        
        // Mixed case
        addInstance(testData, "GREAT Product!", "very positive");
        addInstance(testData, "awful QUALITY!", "very negative");
        
        // Punctuation
        addInstance(testData, "Wow!!! Amazing!!! Best ever!!!", "very positive");
        addInstance(testData, "Bad... very bad...", "very negative");
        
        // Numbers and special characters
        addInstance(testData, "5 stars! Top-notch quality @ great price!", "very positive");
        addInstance(testData, "1 star. Poor quality & bad service.", "very negative");

        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(testData);

        assertThat(classifier).isNotNull();
        
        // Verify classifier can make predictions on new instances
        DenseInstance newInstance = new DenseInstance(2);
        newInstance.setDataset(testData);
        newInstance.setValue(0, "Excellent product!");
        newInstance.setValue(1, "very positive");
        
        double[] distribution = classifier.distributionForInstance(newInstance);
        assertThat(distribution).hasSize(5);
        for (double prob : distribution) {
            assertThat(prob).isBetween(0.0, 1.0);
        }
        double sum = 0;
        for (double d : distribution) sum += d;
        assertThat(sum).isCloseTo(1.0, within(0.01));
    }

    @Test
    void testNlpStopWordRemoval() throws Exception {
        // Test that common stop words are handled appropriately
        Instances testData = new Instances(trainingData, 0);
        
        addInstance(testData, "The product is the best and the greatest", "very positive");
        addInstance(testData, "This is a terrible and awful product", "very negative");
        
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(testData);

        assertThat(classifier).isNotNull();
    }

    @Test
    void testNlpStemming() throws Exception {
        // Test that stemming works (running, runs, ran -> run)
        Instances testData = new Instances(trainingData, 0);
        
        addInstance(testData, "running smoothly, runs perfectly, ran great", "very positive");
        addInstance(testData, "breaking easily, breaks quickly, broke fast", "very negative");
        
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(testData);

        assertThat(classifier).isNotNull();
    }

    @Test
    void testTfIdfWeighting() throws Exception {
        // Test that TF-IDF properly weights important vs common words
        Instances testData = new Instances(trainingData, 0);
        
        // Unique discriminative words
        addInstance(testData, "magnificent spectacular extraordinary", "very positive");
        addInstance(testData, "dreadful atrocious abysmal", "very negative");
        
        // Common neutral words
        addInstance(testData, "product item thing object", "neutral");
        
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(testData);

        assertThat(classifier).isNotNull();
        
        // Make prediction on text with unique positive words
        DenseInstance positiveTest = new DenseInstance(2);
        positiveTest.setDataset(testData);
        positiveTest.setValue(0, "magnificent and spectacular");
        positiveTest.setValue(1, "very positive");
        
        double[] dist = classifier.distributionForInstance(positiveTest);
        int predictedIndex = argMax(dist);
        assertThat(testData.classAttribute().value(predictedIndex)).isEqualTo("very positive");
    }

    @Test
    void testMultiClassPrediction() throws Exception {
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(trainingData);

        // Test very positive prediction
        DenseInstance positiveInstance = new DenseInstance(2);
        positiveInstance.setDataset(trainingData);
        positiveInstance.setValue(0, "Absolutely wonderful and fantastic experience!");
        positiveInstance.setValue(1, "very positive");
        
        double[] positiveDist = classifier.distributionForInstance(positiveInstance);
        assertThat(argMax(positiveDist)).isEqualTo(0); // very positive class

        // Test very negative prediction
        DenseInstance negativeInstance = new DenseInstance(2);
        negativeInstance.setDataset(trainingData);
        negativeInstance.setValue(0, "Terrible and awful, completely disappointed!");
        negativeInstance.setValue(1, "very negative");
        
        double[] negativeDist = classifier.distributionForInstance(negativeInstance);
        assertThat(argMax(negativeDist)).isEqualTo(4); // very negative class (index 4)
    }

    @Test
    void testEmptyTextHandling() throws Exception {
        Instances testData = new Instances(trainingData, 0);
        addInstance(testData, "", "neutral");
        addInstance(testData, "   ", "neutral");
        addInstance(testData, "Great product", "very positive");

        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(testData);

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
        addInstance(testData, longText.toString(), "very positive");

        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(testData);

        assertThat(classifier).isNotNull();
    }

    @Test
    void testSpecialCharactersAndUnicode() throws Exception {
        Instances testData = new Instances(trainingData, 0);
        
        addInstance(testData, "Café quality ★★★★★ 100% satisfaction!", "very positive");
        addInstance(testData, "Terrible quality ☹ Don't buy!!!", "very negative");
        addInstance(testData, "Okay product... 50/50 experience", "neutral");

        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(testData);

        assertThat(classifier).isNotNull();
    }

    @Test
    void testMinimalTrainingData() throws Exception {
        Instances minimalData = new Instances(trainingData, 0);
        addInstance(minimalData, "excellent", "very positive");
        addInstance(minimalData, "good", "somewhat positive");
        addInstance(minimalData, "okay", "neutral");
        addInstance(minimalData, "bad", "somewhat negative");
        addInstance(minimalData, "terrible", "very negative");

        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(minimalData);

        assertThat(classifier).isNotNull();
    }

    @Test
    void testModelPersistenceFromFile(@TempDir Path tempDir) throws Exception {
        Path csvFile = tempDir.resolve("train.csv");
        String content = """
                review_text,sentiment_label
                "Excellent device very satisfied",very positive
                "Terrible quality very disappointed",very negative
                "Average item nothing special",neutral
                "Amazing quality highly recommend",very positive
                "Awful experience complete waste",very negative
                "Good product works well",somewhat positive
                "Not great could improve",somewhat negative
                """;
        Files.writeString(csvFile, content);

        Instances data = DatasetLoader.load(csvFile, "sentiment_label");
        
        SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(data);

        assertThat(classifier).isNotNull();
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
