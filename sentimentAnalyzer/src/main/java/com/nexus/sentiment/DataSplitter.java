package com.nexus.sentiment;

import weka.core.Instances;

import java.util.Random;

/**
 * Provides deterministic train/test splits for Instances.
 */
public final class DataSplitter {
    private DataSplitter() {
    }

    /**
     * Splits the given dataset into training and testing sets based on the specified ratio.
     *
     * @param data: The full dataset to be split (Weka Instances object)
     * @param trainRatio: Ratio of the dataset to be used for training (must be between 0 and 1)
     * @param seed: Random seed for shuffling the dataset
     * @return A Split record containing the training and testing sets
     * @throws IllegalArgumentException if trainRatio is not between 0 and 1
     */
    public static Split split(Instances data, double trainRatio, long seed) {
        // Validate that the train ratio is within (0, 1)
        if (trainRatio <= 0 || trainRatio >= 1) {
            throw new IllegalArgumentException("Train ratio must be within (0,1)");
        }

        // Create a copy of the dataset to shuffle, leaving the original intact
        Instances shuffled = new Instances(data);
        shuffled.randomize(new Random(seed));
        int numInstances = shuffled.numInstances();
        int trainSize = (int) Math.round(numInstances * trainRatio);
        if (trainSize == 0 && numInstances > 0) {
            trainSize = 1;
        }
        if (trainSize == numInstances && numInstances > 1) {
            trainSize = numInstances - 1;
        }
        int testSize = numInstances - trainSize;

        // Create training and testing subsets
        Instances train = new Instances(shuffled, 0, trainSize);
        Instances test = new Instances(shuffled, trainSize, testSize);

        // Return the result as a Split record
        return new Split(train, test);
    }

    /**
     * Immutable record representing the result of a dataset split.
     * Contains training and testing subsets.
     */
    public record Split(Instances train, Instances test) {
    }
}
