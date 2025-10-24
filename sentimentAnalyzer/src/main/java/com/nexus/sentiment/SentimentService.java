package com.nexus.sentiment;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Attribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

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

  private synchronized void ensureLoaded() {
    if (classifier != null && scoreMapper != null) {
      return;
    }
    try {
      Instances data = DatasetLoader.load(
        Paths.get("src/main/resources/data/sample_reviews.csv"),
        "sentiment_label"
      );
      scoreMapper = ScoreMapper.fromAttribute(data.classAttribute());
      classifier = trainer.train(data, "review_text");
    } catch (Exception e) {
      throw new RuntimeException("Failed to load/train sentiment model", e);
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
  ensureLoaded();
  try {
  Instances header = buildHeaderInstances();
  Instance inst = new DenseInstance(header.numAttributes());
  inst.setDataset(header);
  Attribute textAttr = header.attribute("review_text");
  if (textAttr != null && textAttr.isString()) {
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

  private Instances buildHeaderInstances() {
  try {
  Instances data = DatasetLoader.load(
    Paths.get("src/main/resources/data/sample_reviews.csv"),
    "sentiment_label"
  );
  return new Instances(data, 0); // Empty header-only dataset
  } catch (Exception e) {
  throw new RuntimeException(e);
  }
  }
}
