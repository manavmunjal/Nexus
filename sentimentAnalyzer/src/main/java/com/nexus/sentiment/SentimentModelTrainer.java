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

public class SentimentModelTrainer {

    private FilteredClassifier classifier;

    /**
     * Trains a Support Vector Machine model on the dataset with text preprocessing.
     *
     * @param trainData Raw dataset with text and class label.
     * @param textAttributeName Name of the text attribute (e.g. "review_text").
     * @return FilteredClassifier trained classifier.
     * @throws Exception if training fails.
     */
    public FilteredClassifier train(Instances trainData, String textAttributeName) throws Exception {
        // if text attribute does not exist, throw exception
        if (trainData.attribute(textAttributeName) == null) {
            throw new IllegalArgumentException("Unrecognized Text attribute: " + textAttributeName);
        }

        // if instances less than 5, throw exception
        if (trainData.numInstances() < 5) {
            throw new IllegalArgumentException("Insufficient training instances: " + trainData.numInstances());
        }
        
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
        stringToWordVector.setWordsToKeep(5000);
        stringToWordVector.setOutputWordCounts(true);

        // Use a stopwords handler
        // WordsFromFile stopwords = new WordsFromFile();
        // stopwords.setStopwords(new File("resources/stopwords.txt"));
        // stringToWordVector.setStopwordsHandler(stopwords);

        // Optional: Use N-grams (1–2 grams)
        NGramTokenizer tokenizer = new NGramTokenizer();
        tokenizer.setNGramMinSize(1);
        tokenizer.setNGramMaxSize(2);
        tokenizer.setDelimiters("\\W");
        stringToWordVector.setTokenizer(tokenizer);

        // Stemmer
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

        // Tune C and/or gamma using CVParameterSelection
        CVParameterSelection cvParams = new CVParameterSelection();
        cvParams.setClassifier(smo);
        cvParams.setNumFolds(5);
        cvParams.addCVParameter("C 0.1 5.0 5");
        // cvParams.addCVParameter("G 0.001 0.1 5");
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
