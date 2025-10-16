package com.nexus.sentiment;

import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.core.stemmers.SnowballStemmer;
import weka.core.stopwords.Rainbow;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.NominalToString;
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
        
        // First, filter out non-target string and nominal attributes
        Instances filtered = removeNonTargetStringAndNominalAttributes(trainData);

        // Ensure the text attribute is String type (CSV may load it as nominal)
        if (filtered.attribute(textAttributeName).isNominal()) {
            NominalToString nominalToString = new NominalToString();
            nominalToString.setAttributeIndexes(Integer.toString(filtered.attribute(textAttributeName).index() + 1));
            nominalToString.setInputFormat(filtered);
            filtered = Filter.useFilter(filtered, nominalToString);
        }

        int attributeIndex = filtered.attribute(textAttributeName).index() + 1; // StringToWordVector uses 1-based indexes

        StringToWordVector vectorizer = new StringToWordVector();
        vectorizer.setAttributeIndices(Integer.toString(attributeIndex));
        vectorizer.setTFTransform(true);
        vectorizer.setIDFTransform(true);
        vectorizer.setLowerCaseTokens(true);
        vectorizer.setWordsToKeep(5000);
        vectorizer.setOutputWordCounts(true);
    SnowballStemmer stemmer = new SnowballStemmer();
    stemmer.setStemmer("english");
    vectorizer.setStemmer(stemmer);
        vectorizer.setStopwordsHandler(new Rainbow());

        NaiveBayesMultinomial nb = new NaiveBayesMultinomial();

        classifier = new FilteredClassifier();
        classifier.setFilter(vectorizer);
        classifier.setClassifier(nb);
        classifier.buildClassifier(filtered);
        return classifier;
    }

    private Instances removeNonTargetStringAndNominalAttributes(Instances data) throws Exception {
        StringBuilder indicesToRemove = new StringBuilder();
        boolean first = true;
        
        for (int i = 0; i < data.numAttributes(); i++) {
            // Skip the text attribute and the class attribute
            boolean isTextAttribute = data.attribute(i).name().equals(textAttributeName);
            boolean isClassAttribute = i == data.classIndex();
            boolean isStringAttribute = data.attribute(i).isString();
            boolean isNominalAttribute = data.attribute(i).isNominal();
            
            if (!isTextAttribute && !isClassAttribute && (isStringAttribute || isNominalAttribute)) {
                if (!first) {
                    indicesToRemove.append(",");
                }
                indicesToRemove.append(i + 1); // 1-based index for Remove filter
                first = false;
            }
        }
        
        if (indicesToRemove.length() == 0) {
            // No attributes to remove
            return data;
        }
        
        Remove removeFilter = new Remove();
        removeFilter.setAttributeIndices(indicesToRemove.toString());
        removeFilter.setInputFormat(data);
        return Filter.useFilter(data, removeFilter);
    }

    public FilteredClassifier getClassifier() {
        if (classifier == null) {
            throw new IllegalStateException("Classifier has not been trained yet");
        }
        return classifier;
    }
}
