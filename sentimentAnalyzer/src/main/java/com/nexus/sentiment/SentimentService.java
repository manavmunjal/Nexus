package com.nexus.sentiment;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Attribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.InputStream;
import java.net.URL;

@Service
public class SentimentService {

  /**
   * Factor for normalizing sentiment scores.
   */
  private static final double NORMALIZATION_FACTOR = 2.5;

  /**
   * Minimum sentiment score value.
   */
  private static final double MIN_SCORE = 0.0;

  /**
   * Maximum sentiment score value.
   */
  private static final double MAX_SCORE = 5.0;

  /**
   * Classifier used for sentiment prediction.
   */
  private volatile FilteredClassifier classifier;

  /**
   * Mapper to convert sentiment labels to scores.
   */
  private volatile ScoreMapper scoreMapper;

  /**
   * Header schema captured from the training dataset (zero instances).
   */
  private volatile Instances trainedHeader;

  /**
   * Names used during training, needed for inference schema.
   */
  private volatile String trainedTextAttr = "review_text";

  /**
   * Trainer used to build the sentiment model.
   */
  private final SentimentModelTrainer trainer;

  /**
   * Constructs a SentimentService with a provided trainer.
   *
   * @param sentimentTrainer the sentiment model trainer
   */
  @Autowired
  public SentimentService(final SentimentModelTrainer sentimentTrainer) {
    this.trainer = sentimentTrainer;
  }

  /**
   * Constructs a SentimentService with default trainer.
   */
  public SentimentService() {
    this.trainer = new SentimentModelTrainer();
  }

  private synchronized void ensureReady() {
    if (classifier == null || scoreMapper == null || trainedHeader == null) {
      throw new IllegalStateException(
        "Sentiment model not trained yet. Call /api/sentiment/train first.");
    }
  }

  /**
   * Returns true if a trained model is available for inference.
   * @return true if trained model is ready
   */
  public boolean isTrained() {
    return classifier != null && scoreMapper != null && trainedHeader != null;
  }

  /**
   * Trains or retrains the sentiment model using the provided parameters.
   * If any parameter is null or blank, sensible defaults are used.
   *
   * Defaults:
  * - datasetPath: "data/augmented_cleaned_data.csv" (classpath resource)
   * - classAttr:   "sentiment_label"
   * - textAttr:    "review_text"
   *
   * @param datasetPath path to CSV dataset
   * @param classAttr class attribute name in dataset
   * @param textAttr text attribute name in dataset
   */
  public synchronized void trainModel(
      final String datasetPath,
      final String classAttr,
      final String textAttr
  ) {
  final String ds = (datasetPath == null || datasetPath.isBlank())
    ? "data/augmented_cleaned_data.csv"
        : datasetPath;
    final String cls = (classAttr == null || classAttr.isBlank())
        ? "sentiment_label"
        : classAttr;
    final String txt = (textAttr == null || textAttr.isBlank())
        ? "review_text"
        : textAttr;

    Path tmpToDelete = null;
    try {
      Path path = resolveDatasetPath(ds);
      Instances data = DatasetLoader.load(path, cls);
      // Build mapper and train classifier
      scoreMapper = ScoreMapper.fromAttribute(data.classAttribute());
      classifier = trainer.train(data, txt);
      trainedHeader = new Instances(data, 0);
      trainedTextAttr = txt;
    } catch (Exception e) {
      throw new RuntimeException("Failed to train sentiment model", e);
    } finally {
      // Best-effort cleanup if we created a temp file for classpath resource
      if (tmpToDelete != null) {
        try {
          Files.deleteIfExists(tmpToDelete);
        } catch (Exception ignore) { }
      }
    }
  }

  /**
   * Resolve dataset path from either filesystem or classpath.
   * If the provided string points to a readable file, returns it.
   * Otherwise load it from the classpath (e.g., resources/data/...).
   * @param ds dataset path string
   * @return resolved Path to dataset CSV
   * @throws Exception if the dataset cannot be found or accessed
   */
  private Path resolveDatasetPath(final String ds) throws Exception {
    Path p = Paths.get(ds);
    if (Files.exists(p)) {
      return p;
    }
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    URL url = cl.getResource(ds.startsWith("/") ? ds.substring(1) : ds);
    if (url == null) {
      // Try common prefix when given a source-relative path
      String alt = ds.replaceFirst("^src/main/resources/", "");
      url = cl.getResource(alt);
    }
    if (url == null) {
      throw new IllegalArgumentException(
        "Dataset not found at path or classpath: " + ds);
    }
    try (InputStream in = url.openStream()) {
      Path tmp = Files.createTempFile("dataset_", ".csv");
      Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      return tmp;
    }
  }

  /**
   * Calculates sentiment score from text input.
   * This method is designed for extension - subclasses can override
   * to provide custom sentiment scoring implementations.
   *
   * @param text the input text to analyze
   * @return sentiment score between 0 and 5
   */
  public double scoreFromText(final String text) {
    ensureReady();
    try {
      Instances header = new Instances(trainedHeader, 0);
      Instance inst = new DenseInstance(header.numAttributes());
      inst.setDataset(header);
      Attribute textAttr = header.attribute(trainedTextAttr);
      if (textAttr != null && textAttr.isString()) {
        System.out.println("Setting text attribute: " + trainedTextAttr + " to value: " + text);
        inst.setValue(textAttr, text);
      }

      double[] dist = classifier.distributionForInstance(inst);
      double expected = 0.0;
      for (int i = 0; i < dist.length; i++) {
        String label = header.classAttribute().value(i);
        expected += dist[i] * scoreMapper.scoreFor(label);
      }

      // Map -1..1 → 0..5
      double normalized = (expected + 1.0) * NORMALIZATION_FACTOR;
      if (normalized < MIN_SCORE) {
        normalized = MIN_SCORE;
      }
      if (normalized > MAX_SCORE) {
        normalized = MAX_SCORE;
      }

      return normalized;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
