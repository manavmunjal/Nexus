package com.nexus.sentiment;

import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.attribute.NominalToString;

import java.nio.file.Path;

public final class DatasetLoader {
    private DatasetLoader() {
    }

    public static Instances load(Path csvPath, String classAttributeName) throws Exception {
        CSVLoader loader = new CSVLoader();
        loader.setFieldSeparator(",");
        loader.setSource(csvPath.toFile());

        Instances data = loader.getDataSet();

        if (data.attribute(classAttributeName) == null) {
            throw new IllegalArgumentException("Missing class attribute: " + classAttributeName);
        }

        // Set class attribute
        data.setClass(data.attribute(classAttributeName));

        // --- Force class to nominal ---
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

        return data;
    }
}
