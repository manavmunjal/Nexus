package com.nexus.service;

import com.nexus.sentiment.DatasetLoader;
import com.nexus.sentiment.ScoreMapper;
import com.nexus.sentiment.SentimentModelTrainer;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Attribute;

import org.springframework.stereotype.Service;

import java.nio.file.Paths;
// ...existing code...

@Service
public class SentimentService {
    private volatile FilteredClassifier classifier;
    private volatile ScoreMapper scoreMapper;

    public SentimentService() {
    }

    private synchronized void ensureLoaded() {
        if (classifier != null && scoreMapper != null) return;
        try {
            Instances data = DatasetLoader.load(Paths.get("src/main/resources/data/sample_reviews.csv"), "sentiment_label");
            scoreMapper = ScoreMapper.fromAttribute(data.classAttribute());
            SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
            // train on all data for a quick model used by API
            classifier = trainer.train(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load/train sentiment model", e);
        }
    }

    /**
     * Returns a score in the range 0..5 for the given text.
     */
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
            // map -1..1 -> 0..5
            double normalized = (expected + 1.0) * 2.5;
            if (normalized < 0) normalized = 0;
            if (normalized > 5) normalized = 5;
            return normalized;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Instances buildHeaderInstances() {
        // build a minimal Instances with the same attributes as training data
        try {
            Instances data = DatasetLoader.load(Paths.get("src/main/resources/data/sample_reviews.csv"), "sentiment_label");
            // clear rows
            Instances header = new Instances(data, 0);
            return header;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
