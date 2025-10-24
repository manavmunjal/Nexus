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

  private volatile FilteredClassifier classifier;
  private volatile ScoreMapper scoreMapper;
  private final SentimentModelTrainer trainer;

  @Autowired
  public SentimentService(SentimentModelTrainer trainer) {
    this.trainer = trainer;
  }

  public SentimentService() {
    this.trainer = new SentimentModelTrainer();
  }

  private synchronized void ensureLoaded() {
    if (classifier != null && scoreMapper != null) return;
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

  public double scoreFromText(String text) {
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
      double normalized = (expected + 1.0) * 2.5;
      if (normalized < 0) normalized = 0;
      if (normalized > 5) normalized = 5;

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
