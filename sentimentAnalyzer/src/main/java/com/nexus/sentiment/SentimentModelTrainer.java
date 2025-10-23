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

/**
 * Trains an SVM model using text data and preprocessing filters.
 */
public final class SentimentModelTrainer {

    /**
     * The final trained classifier.
     */
    private FilteredClassifier classifier;

    /**
     * Minimum number of training instances required.
     */
    private static final int MIN_INSTANCES = 5;

    /**
     * Maximum number of words to keep in the vocabulary.
     */
    private static final int WORDS_TO_KEEP = 5000;

    /**
     * Gamma value for the RBF kernel in the SVM.
     */
    private static final double DEFAULT_GAMMA = 0.0145;

    /**
     * Number of folds used for cross-validation tuning.
     */
    private static final int CV_FOLDS = 5;

    /**
     * Trains a Support Vector Machine model on the dataset
     * with text preprocessing.
     *
     * @param trainData The raw dataset with text and class label
     * @param textAttributeName The name of the text attribute
     * @return Trained FilteredClassifier model
     * @throws Exception if training fails
     */
    public FilteredClassifier train(
            final Instances trainData,
            final String textAttributeName
    ) throws Exception {

        if (trainData.attribute(textAttributeName) == null) {
            throw new IllegalArgumentException(
                    "Unrecognized Text attribute: " + textAttributeName);
        }

        if (trainData.numInstances() < MIN_INSTANCES) {
            throw new IllegalArgumentException(
                    "Insufficient training instances: "
                            + trainData.numInstances());
        }

        System.out.println("Starting training with text attribute: "
                + textAttributeName);

        if (trainData.classIndex() == -1) {
            throw new IllegalArgumentException("Class attribute not set");
        }

        int classIndex = trainData.classIndex();

        // Remove non-text and non-class attributes
        StringBuilder indicesToRemove = new StringBuilder();
        for (int i = 0; i < trainData.numAttributes(); i++) {
            if (i != classIndex
                    && !trainData.attribute(i).name()
                    .equals(textAttributeName)) {
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

        // Configure StringToWordVector
        StringToWordVector stringToWordVector = new StringToWordVector();
        stringToWordVector.setAttributeIndices("first-last");
        stringToWordVector.setTFTransform(true);
        stringToWordVector.setIDFTransform(true);
        stringToWordVector.setLowerCaseTokens(true);
        stringToWordVector.setWordsToKeep(WORDS_TO_KEEP);
        stringToWordVector.setOutputWordCounts(true);

        NGramTokenizer tokenizer = new NGramTokenizer();
        tokenizer.setNGramMinSize(1);
        tokenizer.setNGramMaxSize(2);
        tokenizer.setDelimiters("\\W");
        stringToWordVector.setTokenizer(tokenizer);

        stringToWordVector.setStemmer(new IteratedLovinsStemmer());

        MultiFilter multiFilter = new MultiFilter();
        multiFilter.setFilters(new Filter[]{removeFilter, stringToWordVector});
        multiFilter.setInputFormat(trainData);
        Instances filteredTrainData =
                Filter.useFilter(trainData, multiFilter);

        SMO smo = new SMO();
        RBFKernel rbf = new RBFKernel();
        rbf.setGamma(DEFAULT_GAMMA);
        smo.setKernel(rbf);

        CVParameterSelection cvParams = new CVParameterSelection();
        cvParams.setClassifier(smo);
        cvParams.setNumFolds(CV_FOLDS);
        cvParams.addCVParameter("C 0.1 5.0 5");
        cvParams.buildClassifier(filteredTrainData);

        System.out.println("Best parameters found: "
                + Utils.joinOptions(cvParams.getBestClassifierOptions()));

        SMO tunedSmo = new SMO();
        tunedSmo.setOptions(cvParams.getBestClassifierOptions());
        tunedSmo.setKernel(rbf);

        FilteredClassifier tunedClassifier = new FilteredClassifier();
        tunedClassifier.setFilter(multiFilter);
        tunedClassifier.setClassifier(tunedSmo);
        tunedClassifier.buildClassifier(trainData);

        this.classifier = tunedClassifier;

        System.out.println("Model trained successfully with RBF kernel "
                + "and tuning!");
        return classifier;
    }

    /**
     * Gets the trained FilteredClassifier instance.
     *
     * @return The trained classifier
     */
    public FilteredClassifier getClassifier() {
        return classifier;
    }
}
