package com.nexus.sentiment;

import weka.core.Instances;

import java.util.Random;

/**
 * Provides deterministic train/test splits for Instances.
 */
public final class DataSplitter {
    private DataSplitter() {
    }

    public static Split split(Instances data, double trainRatio, long seed) {
        if (trainRatio <= 0 || trainRatio >= 1) {
            throw new IllegalArgumentException("Train ratio must be within (0,1)");
        }

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

        Instances train = new Instances(shuffled, 0, trainSize);
        Instances test = new Instances(shuffled, trainSize, testSize);

        return new Split(train, test);
    }

    public record Split(Instances train, Instances test) {
    }
}
