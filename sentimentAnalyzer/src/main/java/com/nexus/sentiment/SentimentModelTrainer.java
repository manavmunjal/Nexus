package com.nexus.sentiment;

import weka.classifiers.functions.SMO;
import weka.classifiers.meta.CVParameterSelection;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.core.Utils;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.core.tokenizers.NGramTokenizer;
import weka.core.stopwords.WordsFromFile;

import java.io.File;

public class SentimentModelTrainer {

    private FilteredClassifier classifier;

    /**
     * Trains a NaiveBayesMultinomial model on the dataset with text preprocessing.
     *
     * @param trainData         Raw dataset with text and class label.
     * @param textAttributeName Name of the text attribute (e.g. "review_text").
     * @return FilteredClassifier trained classifier.
     * @throws Exception if training fails.
     */
    public FilteredClassifier train(Instances trainData, String textAttributeName) throws Exception {
        System.out.println("Starting training with text attribute: " + textAttributeName);

        if (trainData.classIndex() == -1) {
            throw new IllegalArgumentException("Class attribute not set");
        }

        int classIndex = trainData.classIndex();

        // Build Remove filter to keep only textAttribute and class attribute
        StringBuilder indicesToRemove = new StringBuilder();
        for (int i = 0; i < trainData.numAttributes(); i++) {
            if (i != classIndex && !trainData.attribute(i).name().equals(textAttributeName)) {
                indicesToRemove.append(i + 1).append(",");
            }
        }

        Remove removeFilter = new Remove();
        if (indicesToRemove.length() > 0) {
            indicesToRemove.deleteCharAt(indicesToRemove.length() - 1); // Remove trailing comma
            removeFilter.setAttributeIndices(indicesToRemove.toString());
        } else {
            removeFilter.setAttributeIndices("");
        }
        removeFilter.setInvertSelection(false);

        // Configure StringToWordVector
        StringToWordVector stringToWordVector = new StringToWordVector();
        stringToWordVector.setAttributeIndices("first-last");
        stringToWordVector.setTFTransform(true);
        stringToWordVector.setIDFTransform(true);
        stringToWordVector.setLowerCaseTokens(true);
        stringToWordVector.setWordsToKeep(10000); // Increased from 5000 for more features
        stringToWordVector.setOutputWordCounts(true);
        stringToWordVector.setMinTermFreq(2); // Filter out rare words (appear < 2 times)

        // Use built-in stopwords handler (Rainbow stopwords)
        weka.core.stopwords.Rainbow stopwords = new weka.core.stopwords.Rainbow();
        stringToWordVector.setStopwordsHandler(stopwords);

        // Use N-grams (1–2 grams) - bigrams capture phrase sentiment better
        NGramTokenizer tokenizer = new NGramTokenizer();
        tokenizer.setNGramMinSize(1);
        tokenizer.setNGramMaxSize(2); // Changed from 3 to 2 (unigrams + bigrams)
        tokenizer.setDelimiters("\\W");
        stringToWordVector.setTokenizer(tokenizer);

        // Optional: Stemmer (can comment out if hurting performance)
        stringToWordVector.setStemmer(new IteratedLovinsStemmer());

        // Combine filters into MultiFilter
        MultiFilter multiFilter = new MultiFilter();
        multiFilter.setFilters(new Filter[]{removeFilter, stringToWordVector});
        multiFilter.setInputFormat(trainData);
        Instances filteredTrainData = Filter.useFilter(trainData, multiFilter);

        // Configure SVM with RBF kernel
        SMO smo = new SMO();
        RBFKernel rbf = new RBFKernel();
        rbf.setGamma(0.0145); // Can tune this if needed
        smo.setKernel(rbf);

        // Tune C and gamma using CVParameterSelection with broader ranges
        CVParameterSelection cvParams = new CVParameterSelection();
        cvParams.setClassifier(smo);
        cvParams.setNumFolds(10); // Increased from 5 for more robust validation
        cvParams.addCVParameter("C 0.1 100 10"); // Wider range: 0.1 to 100, 10 steps

        // Tune gamma for RBF kernel - critical for performance
        cvParams.addCVParameter("K \"weka.classifiers.functions.supportVector.RBFKernel -G 0.01 1.0 10\"");

        cvParams.buildClassifier(filteredTrainData);

        // Print best parameters
        System.out.println("Best parameters found: " + Utils.joinOptions(cvParams.getBestClassifierOptions()));

        // Create final SMO classifier with tuned params
        SMO tunedSmo = new SMO();
        tunedSmo.setOptions(cvParams.getBestClassifierOptions());
        tunedSmo.setKernel(rbf); // Ensure RBF kernel is set again

        // Final classifier wrapped with MultiFilter
        FilteredClassifier tunedClassifier = new FilteredClassifier();
        tunedClassifier.setFilter(multiFilter);
        tunedClassifier.setClassifier(tunedSmo);

        tunedClassifier.buildClassifier(trainData);
        this.classifier = tunedClassifier;

        System.out.println("Model trained successfully with RBF kernel and tuning!");
        return classifier;
    }

    public FilteredClassifier getClassifier() {
        return classifier;
    }
}
