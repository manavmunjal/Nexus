package com.nexus.sentiment;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

@Service
public final class SentimentService {

  /** Logger instance for the class. */
  private static final Logger LOGGER = LoggerFactory
  .getLogger(SentimentService.class);

  /**
   * Classifier used for sentiment prediction.
   */
  private FilteredClassifier classifier;

  /**
   * Mapper to convert sentiment labels to scores.
   */
  private ScoreMapper scoreMapper;

  /**
   * Header schema captured from the training dataset (zero instances).
   */
  private Instances trainedHeader;

  /**
   * Names used during training, needed for inference schema.
   */
  private String trainedTextAttr = "review_text";

  /**
   * Trainer used to build the sentiment model.
   */
  private final SentimentModelTrainer trainer;

  /**
   * File paths for saving/loading the model components.
   */
  private final String classifierFile;

  /**
   * File paths for saving/loading the model components.
   */
  private final String headerFile;

  /**
   * File paths for saving/loading the model components.
   */
  private final String scoresFile;
  /**
   * Directory where the sentiment model files are stored.
   */
  private final String modelDir;


  /**
   * Constructs a SentimentService with a provided trainer.
   *
   * @param newTrainer the sentiment model trainer
   * @param newModelDir the directory to save/load the model files
   */
  @Autowired
  public SentimentService(final SentimentModelTrainer newTrainer,
   final @Value("${sentiment.modelDir}") String newModelDir) {
    this.trainer = newTrainer;
    this.modelDir = newModelDir;
    this.classifierFile = newModelDir + "sentiment_classifier.model";
    this.headerFile = newModelDir + "sentiment_header.model";
    this.scoresFile = newModelDir + "sentiment_scores.model";
  }

  /**
   * Constructs a SentimentService with a provided trainer
   * and default model directory.
   *
   * @param newTrainer the sentiment model trainer
   */
  public SentimentService(final SentimentModelTrainer newTrainer) {
    this(newTrainer, "saved_models/");
  }

  /**
   * Constructs a SentimentService with a default trainer.
   */
  public SentimentService() {
    this(new SentimentModelTrainer());
  }

  private synchronized void ensureReady() {
    if (classifier == null || scoreMapper == null || trainedHeader == null) {
      throw new IllegalStateException(
        "Sentiment model not trained yet. Call /api/sentiment/train first.");
    }
  }

  /**
   * Gets the directory where the sentiment model is stored.
   * @return the model directory path
   */
  public String getModelDir() {
    return modelDir;
  }

  /**
   * Gets the trained FilteredClassifier model.
   * @return the trained FilteredClassifier
   */
  public FilteredClassifier getClassifier() {
    return classifier;
  }

  /**
   * Gets the Instances header used during training.
   * @return the trained Instances header
   */
  public Instances getTrainedHeader() {
      return trainedHeader;
  }

  /**
   * Gets the name of the text attribute used during training.
   * @return the text attribute name
   */
  public String getTrainedTextAttr() {
      return trainedTextAttr;
  }

  /**
   * Gets the ScoreMapper used for label-to-score conversion.
   * @return the ScoreMapper instance
   */
  public ScoreMapper getScoreMapper() {
      return scoreMapper;
  }

  void setClassifier(final FilteredClassifier newClassifier) {
    this.classifier = newClassifier;
  }

  void setTrainedHeader(final Instances newTrainedHeader) {
    this.trainedHeader = newTrainedHeader;
  }

  void setScoreMapper(final ScoreMapper newScoreMapper) {
    this.scoreMapper = newScoreMapper;
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
   * @throws RuntimeException if any of the model components cannot be
   * serialized or written
   */
  public synchronized void saveModel() {
    try {
        File dir = new File(modelDir);
        if (!dir.exists()) {
          dir.mkdirs();
        }
        SerializationHelper.write(classifierFile, classifier);
        SerializationHelper.write(headerFile, trainedHeader);
        SerializationHelper.write(scoresFile, scoreMapper);
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
    final String ds = datasetPath == null || datasetPath.isBlank()
        ? "data/augmented_cleaned_data.csv"
        : datasetPath;
    final String cls = classAttr == null || classAttr.isBlank()
        ? "sentiment_label"
        : classAttr;
    final String txt = textAttr == null || textAttr.isBlank()
        ? "review_text"
        : textAttr;

    Path tmpToDelete = null;
    try {
      Path path = resolveDatasetPath(ds);
      Instances data = DatasetLoader.load(path, cls, txt);
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
        } catch (Exception e) {
          LOGGER.warn("Failed to delete temporary file: {}", tmpToDelete, e);
        }
      }
    }
  }

  /**
   * Loads a previously trained sentiment model from disk.
   * Expects the model, header, and score mapper files to
   * exist in the configured model directory.
   *
   * Files required:
   * - CLASSIFIER_FILE: the serialized FilteredClassifier
   * - HEADER_FILE: the Instances header from training
   * - SCORES_FILE: the ScoreMapper object mapping labels to scores
   *
   * After successful loading, the model is ready for
   * inference via scoreFromText().
   *
   * @throws RuntimeException if any of the model files are missing or
   * cannot be read.
   */
  public synchronized void loadModel() {
    try {
        File c = new File(classifierFile);
        File h = new File(headerFile);
        File s = new File(scoresFile);
        if (c.exists() && h.exists() && s.exists()) {
            classifier
            = (FilteredClassifier) SerializationHelper.read(classifierFile);
            trainedHeader = (Instances) SerializationHelper.read(headerFile);
            scoreMapper = (ScoreMapper) SerializationHelper.read(scoresFile);
        } else {
            throw new RuntimeException("Saved model files not found in "
            + modelDir);
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
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Label: {}, Probability: {}, Score: {}",
          label, dist[i], scoreMapper.scoreFor(label));
        }
        expected += dist[i] * scoreMapper.scoreFor(label);
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Expected score (raw): {}", expected);
      }

      return expected;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
