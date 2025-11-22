package com.nexus.sentiment;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Attribute;
import weka.core.SerializationHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.InputStream;
import java.net.URL;

@Service
public class SentimentService {

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

  private static final String MODEL_DIR = "./saved_models/";
  private static final String CLASSIFIER_FILE = MODEL_DIR + "sentiment_classifier.model";
  private static final String HEADER_FILE = MODEL_DIR + "sentiment_header.model";
  private static final String SCORES_FILE = MODEL_DIR + "sentiment_scores.model";


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
   * Saves the currently trained sentiment model to disk.
   * 
   * Files created:
   * - CLASSIFIER_FILE: serialized FilteredClassifier
   * - HEADER_FILE: serialized Instances header
   * - SCORES_FILE: serialized ScoreMapper object mapping labels to scores
   *
   * Ensures the model directory exists before writing.
   *
   * @throws RuntimeException if any of the model components cannot be serialized or written
   */
  public synchronized void saveModel() {
    try {
        File dir = new File(MODEL_DIR);
        if (!dir.exists()) dir.mkdirs();

        SerializationHelper.write(CLASSIFIER_FILE, classifier);
        SerializationHelper.write(HEADER_FILE, trainedHeader);
        SerializationHelper.write(SCORES_FILE, scoreMapper);
    } catch (Exception e) {
        throw new RuntimeException("Failed to save sentiment model", e);
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
      data = SentimentLabelConverter.convertTo3Class(data, cls);
      // Build mapper and train classifier
      scoreMapper = ScoreMapper.fromAttribute(data.classAttribute());
      classifier = trainer.train(data, txt);
      trainedHeader = new Instances(data, 0);
      trainedTextAttr = txt;
      
      saveModel();

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
   * Loads a previously trained sentiment model from disk.
   * Expects the model, header, and score mapper files to exist in the configured model directory.
   *
   * Files required:
   * - CLASSIFIER_FILE: the serialized FilteredClassifier
   * - HEADER_FILE: the Instances header from training
   * - SCORES_FILE: the ScoreMapper object mapping labels to scores
   *
   * After successful loading, the model is ready for inference via scoreFromText().
   *
   * @throws RuntimeException if any of the model files are missing or cannot be read
   */
  public synchronized void loadModel() {
    try {
        File c = new File(CLASSIFIER_FILE);
        File h = new File(HEADER_FILE);
        File s = new File(SCORES_FILE);
        if (c.exists() && h.exists() && s.exists()) {
            classifier = (FilteredClassifier) SerializationHelper.read(CLASSIFIER_FILE);
            trainedHeader = (Instances) SerializationHelper.read(HEADER_FILE);
            scoreMapper = (ScoreMapper) SerializationHelper.read(SCORES_FILE);
        } else {
            throw new RuntimeException("Saved model files not found in " + MODEL_DIR);
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to load sentiment model", e);
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
        inst.setValue(textAttr, text);
      }

      double[] dist = classifier.distributionForInstance(inst);
      double expected = 0.0;
      for (int i = 0; i < dist.length; i++) {
        String label = header.classAttribute().value(i);
        System.out.println("Label: " + label + ", Probability: "
                        + dist[i] + ", Score: " + scoreMapper.scoreFor(label));
        expected += dist[i] * scoreMapper.scoreFor(label);
      }
      System.out.println("Expected score (raw): " + expected);

      return expected;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
