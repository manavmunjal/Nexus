package com.nexus.sentiment;

import weka.classifiers.functions.SMO;
import weka.classifiers.meta.CVParameterSelection;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.core.Utils;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class SentimentModelTrainer {

    private FilteredClassifier classifier;

    /**
     * Trains an SVM (SMO) model with linear kernel on the dataset,
     * including text preprocessing and parameter tuning (C).
     *
     * @param trainData          Raw dataset with text and class label.
     * @param textAttributeName  Name of the text attribute (e.g. "review_text").
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

        // StringToWordVector for text processing
        StringToWordVector stringToWordVector = new StringToWordVector();
        stringToWordVector.setAttributeIndices("first-last");
        stringToWordVector.setTFTransform(true);
        stringToWordVector.setIDFTransform(true);
        stringToWordVector.setLowerCaseTokens(true);
        stringToWordVector.setWordsToKeep(5000);
        stringToWordVector.setOutputWordCounts(true);
        stringToWordVector.setStemmer(new IteratedLovinsStemmer());

        // Combine filters into a MultiFilter
        MultiFilter multiFilter = new MultiFilter();
        multiFilter.setFilters(new Filter[]{removeFilter, stringToWordVector});

        // IMPORTANT: set input format for filtering the data BEFORE tuning
        multiFilter.setInputFormat(trainData);
        Instances filteredTrainData = Filter.useFilter(trainData, multiFilter);

        // SMO classifier with linear kernel (PolyKernel exponent 1)
        SMO smo = new SMO();
        PolyKernel linearKernel = new PolyKernel();
        linearKernel.setExponent(1);
        smo.setKernel(linearKernel);

        // Parameter tuning with CVParameterSelection on filtered (numeric) data
        CVParameterSelection cvParams = new CVParameterSelection();
        cvParams.setClassifier(smo);
        cvParams.setNumFolds(5);  // 5-fold cross-validation for tuning

        // Tune C from 1 to 10 in 5 steps
        cvParams.addCVParameter("C 1 10 5");

        // Build classifier with parameter tuning on filtered data
        cvParams.buildClassifier(filteredTrainData);

        // Print best parameters
        System.out.println("Best parameters found: " + Utils.joinOptions(cvParams.getBestClassifierOptions()));

        // Create a new SMO classifier and set the best options found
        SMO tunedSmo = new SMO();
        tunedSmo.setOptions(cvParams.getBestClassifierOptions());
        tunedSmo.setKernel(linearKernel);  // Re-set kernel to linear

        // Wrap tuned classifier with filters again (to apply on raw data at prediction)
        FilteredClassifier tunedClassifier = new FilteredClassifier();
        tunedClassifier.setFilter(multiFilter);
        tunedClassifier.setClassifier(tunedSmo);

        // Build final classifier on the whole training set (raw data)
        tunedClassifier.buildClassifier(trainData);

        classifier = tunedClassifier;

        System.out.println("Model trained successfully with parameter tuning!");

        return classifier;
    }

    public FilteredClassifier getClassifier() {
        return classifier;
    }
}