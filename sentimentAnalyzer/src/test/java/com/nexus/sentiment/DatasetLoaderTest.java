package com.nexus.sentiment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import weka.core.Instances;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class DatasetLoaderTest {

    @Test
    void testLoadValidDataset(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        String content = """
                review_id,review_text,sentiment_label
                1,"Great product",very positive
                2,"Bad quality",very negative
                3,"Okay item",neutral
                4,"Pretty good",somewhat positive
                5,"Not great",somewhat negative
                """;
        Files.writeString(csvFile, content);

        Instances data = DatasetLoader.load(csvFile, "sentiment_label");

        assertThat(data).isNotNull();
        assertThat(data.numInstances()).isEqualTo(5);
        assertThat(data.classIndex()).isGreaterThanOrEqualTo(0);
        assertThat(data.classAttribute().name()).isEqualTo("sentiment_label");
    }

    @Test
    void testLoadMissingClassAttribute(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        String content = """
                review_id,review_text
                1,"Great product"
                """;
        Files.writeString(csvFile, content);

        assertThatThrownBy(() -> DatasetLoader.load(csvFile, "sentiment_label"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing class attribute");
    }

    @Test
    void testLoadNonExistentFile(@TempDir Path tempDir) {
        Path csvFile = tempDir.resolve("nonexistent.csv");

        assertThatThrownBy(() -> DatasetLoader.load(csvFile, "sentiment_label"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void testLoadEmptyDataset(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("empty.csv");
        String content = "review_id,review_text,sentiment_label\n";
        Files.writeString(csvFile, content);

        Instances data = DatasetLoader.load(csvFile, "sentiment_label");

        assertThat(data.numInstances()).isEqualTo(0);
        assertThat(data.classAttribute().name()).isEqualTo("sentiment_label");
    }
}
