package com.nexus.sentiment;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.attribute.NominalToString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
     * @return Instances object loaded from the CSV.
     * @throws Exception If loading or parsing fails.
     */
    public static Instances load(
            final Path csvPath,
            final String classAttributeName) throws Exception {

        CSVLoader loader = new CSVLoader();
        loader.setFieldSeparator(",");

        Path sourcePath = csvPath;
        boolean createdTemp = false;

        List<String> lines = Files.exists(csvPath)
                ? Files.readAllLines(csvPath)
                : List.of();

        if (!lines.isEmpty()) {
            String header = lines.get(0).trim();
            boolean singleColumn = !header.contains(",");

            if ("review_text".equals(header) && singleColumn) {
                List<String> cleaned = new ArrayList<>(lines.size());
                cleaned.add(header);
                for (int i = 1; i < lines.size(); i++) {
                    String row = lines.get(i);
                    String noQuotes = row
                            .replace("\"", "")
                            .replace("'", "");
                    String sanitized = noQuotes.replace(",", "");
                    cleaned.add(sanitized);
                }
                Path tmp = Files.createTempFile(
                        "datasetloader_clean_", ".csv");
                Files.write(tmp, cleaned);
                sourcePath = tmp;
                createdTemp = true;
            }
        }

        loader.setSource(sourcePath.toFile());
        Instances data = loader.getDataSet();

        if (createdTemp) {
            try {
                Files.deleteIfExists(sourcePath);
            } catch (Exception e) {
                System.err.println(
                        "Failed to delete temp file: " + sourcePath);
                e.printStackTrace();
            }
        }

        if (data.attribute(classAttributeName) == null) {
            throw new IllegalArgumentException(
                    "Missing class attribute: " + classAttributeName);
        }

        data.setClass(data.attribute(classAttributeName));

        if (!data.classAttribute().isNominal()) {
            StringToNominal strToNom = new StringToNominal();
            strToNom.setAttributeRange(
                    String.valueOf(data.classIndex() + 1));
            strToNom.setInputFormat(data);
            data = Filter.useFilter(data, strToNom);
            data.setClass(data.attribute(classAttributeName));
        }

        Attribute reviewAttr = data.attribute("review_text");

        if (reviewAttr != null && !reviewAttr.isString()) {
            int textAttrIndex = reviewAttr.index() + 1;
            NominalToString nts = new NominalToString();
            nts.setAttributeIndexes(String.valueOf(textAttrIndex));
            nts.setInputFormat(data);
            data = Filter.useFilter(data, nts);
        }

        if (reviewAttr != null && reviewAttr.isString()) {
            for (int i = 0; i < data.numInstances(); i++) {
                String val = data.instance(i).stringValue(reviewAttr);
                if (val != null && !val.isEmpty()) {
                    String cleaned = val
                            .replace("\"", "")
                            .replace("'", "");
                    data.instance(i).setValue(reviewAttr, cleaned);
                }
            }
        }

        return data;
    }
}
