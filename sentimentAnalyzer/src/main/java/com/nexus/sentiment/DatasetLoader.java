package com.nexus.sentiment;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.attribute.NominalToString;
import weka.filters.unsupervised.attribute.NumericToNominal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for loading datasets from CSV files.
 */
public final class DatasetLoader {

  private DatasetLoader() {
    // Prevent instantiation
  }

  /**
     * Loads and preprocesses a dataset from a CSV file.
     *
     * @param csvPath Path to the CSV file.
     * @param classAttributeName Name of the class attribute.
     * @param textAttributeName Name of the text attribute.
     * @return Instances object loaded from the CSV.
     * @throws Exception If loading or parsing fails.
     */
  public static Instances load(
      final Path csvPath,
      final String classAttributeName,
      final String textAttributeName) throws Exception {

    if (!Files.exists(csvPath)) {
      throw new IOException("CSV file does not exist: " + csvPath);
    }

    CSVLoader loader = new CSVLoader();
    loader.setFieldSeparator(",");

    Instances data;
    try {
      loader.setSource(csvPath.toFile());
      data = loader.getDataSet();
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Failed to load dataset from file: " + csvPath, e);
    }

    if (data.numAttributes() < 2) {
      throw new IllegalArgumentException(
          "Dataset must contain at least text and class columns");
    }

    if (data.numInstances() == 0) {
      throw new IllegalArgumentException("Dataset contains no rows");
    }

    Attribute classAttr = data.attribute(classAttributeName);
    if (classAttr == null) {
      throw new IllegalArgumentException(
          "Missing class attribute: " + classAttributeName);
    }

    Attribute textAttr = data.attribute(textAttributeName);
    if (textAttr == null) {
      throw new IllegalArgumentException(
          "Missing text attribute: " + textAttributeName);
    }

    if (textAttr.index() == classAttr.index()) {
      throw new IllegalArgumentException(
          "Text attribute and class attribute must be different");
    }

    boolean anyText = false;
    for (int i = 0; i < data.numInstances(); i++) {
      // Check text
      String textVal = data.instance(i).stringValue(textAttr);
      if (textVal != null && !textVal.equals("?") && !textVal.isBlank()) {
        anyText = true;
      }

      // Check class
      if (data.instance(i).isMissing(classAttr)) {
        throw new IllegalArgumentException(
            "Class attribute contains missing value at row " + (i + 1));
      }
    }

    if (!anyText) {
      throw new IllegalArgumentException(
          "Text attribute contains no usable text");
    }

    data.setClass(classAttr);

    // Convert numeric class to nominal if needed
    if (data.classAttribute().isNumeric()) {
      NumericToNominal numToNom = new NumericToNominal();
      numToNom.setAttributeIndices(String.valueOf(data.classIndex() + 1));
      numToNom.setInputFormat(data);
      data = Filter.useFilter(data, numToNom);
      data.setClass(data.attribute(classAttributeName));
    }

    // Ensure class attribute is nominal
    if (data.classAttribute().isString()) {
      StringToNominal strToNom = new StringToNominal();
      strToNom.setAttributeRange(String.valueOf(data.classIndex() + 1));
      strToNom.setInputFormat(data);
      data = Filter.useFilter(data, strToNom);
      data.setClass(data.attribute(classAttributeName));
    }

    // Ensure text attribute is string
    if (!textAttr.isString()) {
      if (textAttr.isNominal()) {
        NominalToString nts = new NominalToString();
        nts.setAttributeIndexes(String.valueOf(textAttr.index() + 1));
        nts.setInputFormat(data);
        data = Filter.useFilter(data, nts);
        textAttr = data.attribute(textAttributeName);
      } else {
        throw new IllegalArgumentException(
            "Text attribute must be string or nominal");
      }
    }

    // Clean text values
    if (textAttr.isString()) {
      for (int i = 0; i < data.numInstances(); i++) {
        String val = data.instance(i).stringValue(textAttr);
        if (val != null && !val.isEmpty()) {
          String cleaned = val
              .replace("\"", "")
              .replace("\'", "");
          data.instance(i).setValue(textAttr, cleaned);
        }
      }
    }

    return data;
  }
}
