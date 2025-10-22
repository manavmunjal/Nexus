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

public final class DatasetLoader {
    private DatasetLoader() {
    }

    public static Instances load(Path csvPath, String classAttributeName) throws Exception {
        CSVLoader loader = new CSVLoader();
        loader.setFieldSeparator(",");

        // If this looks like a single-column file (e.g., only "review_text"),
        // pre-clean quotes to avoid CSV parsing errors from ill-formatted lines.
        Path sourcePath = csvPath;
        boolean createdTemp = false;
        List<String> lines = Files.exists(csvPath) ? Files.readAllLines(csvPath) : List.of();
        if (!lines.isEmpty()) {
            String header = lines.get(0).trim();
            boolean singleColumn = !header.contains(",");
            if ("review_text".equals(header) && singleColumn) {
                List<String> cleaned = new ArrayList<>(lines.size());
                cleaned.add(header);
                for (int i = 1; i < lines.size(); i++) {
                    String row = lines.get(i);
                    // Remove both double and single quotes in the review text
                    String noQuotes = row.replace("\"", "").replace("'", "");
                    // Also remove commas to ensure single-column compatibility
                    String sanitized = noQuotes.replace(",", "");
                    cleaned.add(sanitized);
                }
                Path tmp = Files.createTempFile("datasetloader_clean_", ".csv");
                Files.write(tmp, cleaned);
                sourcePath = tmp;
                createdTemp = true;
            }
        }

        loader.setSource(sourcePath.toFile());

        Instances data = loader.getDataSet();

        // Clean up temp file if created
        if (createdTemp) {
            try {
                Files.deleteIfExists(sourcePath);
            } catch (Exception e) {
                System.err.println("Failed to delete temp file: " + sourcePath);
                e.printStackTrace();
            }
        }

        if (data.attribute(classAttributeName) == null) {
            throw new IllegalArgumentException("Missing class attribute: " + classAttributeName);
        }

        // Set class attribute
        data.setClass(data.attribute(classAttributeName));

        // Force class to nominal
        if (!data.classAttribute().isNominal()) {
            StringToNominal strToNom = new StringToNominal();
            strToNom.setAttributeRange("" + (data.classIndex() + 1));
            strToNom.setInputFormat(data);
            data = Filter.useFilter(data, strToNom);
            data.setClass(data.attribute(classAttributeName));
        }

        // --- Force review_text to string if needed ---
        if (data.attribute("review_text") != null && !data.attribute("review_text").isString()) {
            int textAttrIndex = data.attribute("review_text").index() + 1;  // 1-based
            NominalToString nts = new NominalToString();
            nts.setAttributeIndexes("" + textAttrIndex);
            nts.setInputFormat(data);
            data = Filter.useFilter(data, nts);
        }

        // --- Remove quotation marks from review_text values if present ---
        // This is necessary for extremely illformatted text inputs.
        Attribute reviewAttr = data.attribute("review_text");
        if (reviewAttr != null && reviewAttr.isString()) {
            for (int i = 0; i < data.numInstances(); i++) {
                String val = data.instance(i).stringValue(reviewAttr);
                if (val != null && !val.isEmpty()) {
                    String cleaned = val.replace("\"", "").replace("'", "");
                    data.instance(i).setValue(reviewAttr, cleaned);
                }
            }
        }

        return data;
    }
}
