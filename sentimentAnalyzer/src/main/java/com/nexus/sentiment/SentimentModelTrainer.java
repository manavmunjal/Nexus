package com.nexus.sentiment;

import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.core.stemmers.SnowballStemmer;
import weka.filters.Filter;

public class SentimentModelTrainer {

    private FilteredClassifier classifier;

    /**
     * Trains a NaiveBayesMultinomial model on the dataset with text preprocessing.
     *
     * @param trainData          Original dataset containing review text and sentiment label.
     * @param textAttributeName  Name of the text attribute (e.g., "review_text").
     * @return FilteredClassifier trained classifier
     * @throws Exception if training or filtering fails.
     */
    public FilteredClassifier train(Instances trainData, String textAttributeName) throws Exception {
        System.out.println("Starting training with text attribute: " + textAttributeName);

        // Ensure class index set
        if (trainData.classIndex() == -1) {
            throw new IllegalArgumentException("Class attribute not set");
        }

        int classIndex = trainData.classIndex();

        // Remove all except textAttribute and class attribute
        StringBuilder indicesToRemove = new StringBuilder();
        for (int i = 0; i < trainData.numAttributes(); i++) {
            if (i != classIndex && !trainData.attribute(i).name().equals(textAttributeName)) {
                indicesToRemove.append(i + 1).append(",");
            }
        }

        Remove removeFilter = new Remove();
        if (indicesToRemove.length() > 0) {
            indicesToRemove.deleteCharAt(indicesToRemove.length() - 1);
            removeFilter.setAttributeIndices(indicesToRemove.toString());
        } else {
            removeFilter.setAttributeIndices("");
        }
        removeFilter.setInvertSelection(false);

        // StringToWordVector setup
        StringToWordVector stringToWordVector = new StringToWordVector();
        int textAttrIndex = trainData.attribute(textAttributeName).index() + 1; // 1-based indexing
        stringToWordVector.setAttributeIndices("first-last");  // Apply to all string attributes
        stringToWordVector.setTFTransform(true);
        stringToWordVector.setIDFTransform(true);
        stringToWordVector.setLowerCaseTokens(true);
        stringToWordVector.setWordsToKeep(5000);
        stringToWordVector.setOutputWordCounts(true);

        // Stemmer setup - lowercase "porter"
        SnowballStemmer stemmer = new SnowballStemmer();
        stemmer.setStemmer("porter"); // still broken here
        stringToWordVector.setStemmer(stemmer);

        // Chain Remove and StringToWordVector filters with MultiFilter
        MultiFilter multiFilter = new MultiFilter();
        multiFilter.setFilters(new Filter[]{removeFilter, stringToWordVector});

        // Build FilteredClassifier with NaiveBayesMultinomial
        NaiveBayesMultinomial nb = new NaiveBayesMultinomial();
        FilteredClassifier classifier = new FilteredClassifier();
        classifier.setFilter(multiFilter);
        classifier.setClassifier(nb);

        // Build classifier on raw training data
        classifier.buildClassifier(trainData);

        System.out.println("Model trained successfully!");
        return classifier;
    }


    public FilteredClassifier getClassifier() {
        return classifier;
    }
}
