package com.nexus.sentiment;

import weka.core.Instances;

import java.util.Random;

/**
 * Provides train/test splits for Instances.
 */
public final class DataSplitter {

  private DataSplitter() {
      // Prevent instantiation
  }

  /**
   * Splits the dataset into training and test sets.
   *
   * @param data Dataset to split.
   * @param trainRatio Ratio of training data (between 0 and 1).
   * @param seed Random seed for shuffling.
   * @return A {@link Split} containing training and test sets.
   */
  public static Split split(
          final Instances data,
          final double trainRatio,
          final long seed) {

      if (data.numInstances() == 0) {
          throw new IllegalArgumentException("Dataset cannot be empty.");
      }

      if (trainRatio <= 0 || trainRatio >= 1) {
          throw new IllegalArgumentException("Train ratio must be within (0,1)");
      }

      if (seed < 0) {
          throw new IllegalArgumentException("Seed must be non-negative.");
      }

      Instances shuffled = new Instances(data);
      shuffled.randomize(new Random(seed));

      int numInstances = shuffled.numInstances();
      int trainSize = (int) Math.round(numInstances * trainRatio);

      // Ensure at least one instance goes into training and testing
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

  /**
   * Holds the training and test splits.
   *
   * @param train Training set.
   * @param test Test set.
   */
  public record Split(Instances train, Instances test) {
  }
}
