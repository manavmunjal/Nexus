package com.nexus.sentiment;

import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.core.stemmers.SnowballStemmer;
import weka.core.stopwords.Rainbow;
import weka.filters.unsupervised.attribute.StringToWordVector;

/**
 * Builds a Naive Bayes pipeline with TF-IDF features for sentiment analysis.
 */
public final class SentimentModelTrainer {
    private final String textAttributeName;
    private FilteredClassifier classifier;

    public SentimentModelTrainer(String textAttributeName) {
        this.textAttributeName = textAttributeName;
    }

    public FilteredClassifier train(Instances trainData) throws Exception {
        if (trainData.attribute(textAttributeName) == null) {
            throw new IllegalArgumentException("Missing text attribute: " + textAttributeName);
        }
        int attributeIndex = trainData.attribute(textAttributeName).index() + 1; // StringToWordVector uses 1-based indexes

        StringToWordVector vectorizer = new StringToWordVector();
        vectorizer.setAttributeIndices(Integer.toString(attributeIndex));
        vectorizer.setTFTransform(true);
        vectorizer.setIDFTransform(true);
        vectorizer.setLowerCaseTokens(true);
        vectorizer.setWordsToKeep(5000);
        vectorizer.setOutputWordCounts(true);
        vectorizer.setStemmer(new SnowballStemmer());
        vectorizer.setStopwordsHandler(new Rainbow());

        NaiveBayesMultinomial nb = new NaiveBayesMultinomial();

        classifier = new FilteredClassifier();
        classifier.setFilter(vectorizer);
        classifier.setClassifier(nb);
        classifier.buildClassifier(trainData);
        return classifier;
    }

    public FilteredClassifier getClassifier() {
        if (classifier == null) {
            throw new IllegalStateException("Classifier has not been trained yet");
        }
        return classifier;
    }
}
