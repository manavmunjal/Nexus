package com.nexus.sentiment;

import weka.core.Instances;
import weka.core.converters.CSVLoader;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads review datasets into Weka Instances and assigns the target class attribute.
 */
public final class DatasetLoader {
    private DatasetLoader() {
    }

    public static Instances load(Path csvPath, String classAttributeName) throws IOException {
        CSVLoader loader = new CSVLoader();
        loader.setSource(csvPath.toFile());
        Instances data = loader.getDataSet();
        if (data.attribute(classAttributeName) == null) {
            throw new IllegalArgumentException("Missing class attribute: " + classAttributeName);
        }
        data.setClass(data.attribute(classAttributeName));
        return data;
    }
}
