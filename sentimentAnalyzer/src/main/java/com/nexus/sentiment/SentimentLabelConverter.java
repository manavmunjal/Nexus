package com.nexus.sentiment;

import weka.core.Instances;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.DenseInstance;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Converts 5-class sentiment labels to 3-class (positive/neutral/negative).
 */
public final class SentimentLabelConverter {

    private SentimentLabelConverter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts 5-class sentiment labels to 3-class labels.
     * Right now SVM model is not suitable for 5-class classification:
     * Mapping:
     * - "very negative"     -> "negative"
     * - "somewhat negative" -> "negative"
     * - "neutral"           -> "neutral"
     * - "somewhat positive" -> "positive"
     * - "very positive"     -> "positive"
     *
     * @param data Original dataset with 5-class labels
     * @param classAttributeName Name of the class attribute
     * @return New dataset with 3-class labels
     * @throws Exception if conversion fails
     */
    public static Instances convertTo3Class(Instances data, String classAttributeName) throws Exception {
        if (data.classIndex() == -1) {
            throw new IllegalArgumentException("Class attribute not set");
        }

        Attribute oldClassAttr = data.classAttribute();

        // Check if already 3-class
        if (oldClassAttr.numValues() == 3 &&
                oldClassAttr.indexOfValue("positive") >= 0 &&
                oldClassAttr.indexOfValue("neutral") >= 0 &&
                oldClassAttr.indexOfValue("negative") >= 0) {
            System.out.println("Dataset already has 3-class labels. No conversion needed.");
            return new Instances(data);
        }

        // Create new class attribute with 3 values
        List<String> classValues = new ArrayList<>();
        classValues.add("negative");
        classValues.add("neutral");
        classValues.add("positive");
        Attribute newClassAttr = new Attribute(classAttributeName, classValues);

        // Create new attribute list (copy all attributes except old class)
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (int i = 0; i < data.numAttributes(); i++) {
            if (i != data.classIndex()) {
                attributes.add((Attribute) data.attribute(i).copy());
            }
        }
        attributes.add(newClassAttr);

        // Create new dataset
        Instances newData = new Instances("3ClassSentiment", new ArrayList<>(attributes), data.numInstances());
        newData.setClassIndex(newData.numAttributes() - 1);

        // Convert each instance
        for (int i = 0; i < data.numInstances(); i++) {
            Instance oldInst = data.instance(i);
            Instance newInst = new DenseInstance(newData.numAttributes());
            newInst.setDataset(newData);

            // Copy all non-class attributes
            int newAttrIdx = 0;
            for (int j = 0; j < data.numAttributes(); j++) {
                if (j != data.classIndex()) {
                    if (data.attribute(j).isString()) {
                        newInst.setValue(newAttrIdx, oldInst.stringValue(j));
                    } else if (data.attribute(j).isNumeric()) {
                        newInst.setValue(newAttrIdx, oldInst.value(j));
                    } else if (data.attribute(j).isNominal()) {
                        newInst.setValue(newAttrIdx, oldInst.stringValue(j));
                    }
                    newAttrIdx++;
                }
            }

            // Convert class value
            String oldLabel = oldInst.stringValue(data.classIndex()).toLowerCase(Locale.ROOT);
            String newLabel = convertLabel(oldLabel);
            newInst.setValue(newData.classIndex(), newLabel);
            newData.add(newInst);
        }

        System.out.println("Converted from " + oldClassAttr.numValues() + "-class to 3-class labels");
        System.out.println("Class distribution:");
        int[] counts = new int[3];
        for (int i = 0; i < newData.numInstances(); i++) {
            String label = newData.instance(i).stringValue(newData.classIndex());
            if ("negative".equals(label)) {
                counts[0]++;
            } else if ("neutral".equals(label)) {
                counts[1]++;
            } else if ("positive".equals(label)) {
                counts[2]++;
            }
        }
        System.out.println(" Negative: " + counts[0]);
        System.out.println(" Neutral: " + counts[1]);
        System.out.println(" Positive: " + counts[2]);

        return newData;
    }

    /**
     * Converts a 5-class label to 3-class label.
     */
    private static String convertLabel(String label) throws Exception {
        switch (label) {
            case "very negative":
            case "somewhat negative": {
                return "negative";
            }
            case "neutral": {
                return "neutral";
            }
            case "somewhat positive":
            case "very positive": {
                return "positive";
            }
            default: {
                // throw exception for unrecognized labels
                throw new IllegalArgumentException("Unrecognized sentiment label: " + label);
            }
        }
    }
}
