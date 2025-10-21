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
    /**
     * Load a valid dataset and verify its contents, and this test
     * verifies the correct case of the dataloader.
     * @param tempDir Temporary directory for test files
     * @throws Exception
     */
    void testLoadValidDataset(@TempDir Path tempDir) throws Exception {
        Path csvFilePath = tempDir.resolve("test.csv");
        String content = """
                review_id,review_text,sentiment_label
                1,"Great product",positive
                2,"Bad quality",negative
                3,"Okay item",neutral
                4,"I really regret for my decision",negative
                5,"what am i even thinking of buying this",negative
                """;
        Files.writeString(csvFilePath, content);

        Instances data = DatasetLoader.load(csvFilePath, "sentiment_label");

        assertThat(data).isNotNull();
        assertThat(data.numInstances()).isEqualTo(5);
        assertThat(data.classIndex()).isGreaterThanOrEqualTo(0);
        assertThat(data.classAttribute().name()).isEqualTo("sentiment_label");
    }

    @Test
    /**
     * Load dataset where columns are out of order. This ensures that even
     * if the provided CSV columns are scrambled, the loader should be able 
     * to correctly identify the class attribute by name.
     * @param tempDir Temporary directory for test files
     * @throws Exception
     */
    void testLoadOutOfColOrderValidDataset(@TempDir Path tempDir) throws Exception {
        Path csvFilePath = tempDir.resolve("test.csv");
        String content = """
                review_id,sentiment_label,review_text
                1,positive,"Great product"
                2,negative,"Bad quality"
                3,neutral,"Okay item"
                4,negative,"I really regret for my decision"
                5,negative,"what am i even thinking of buying this"
                """;
        Files.writeString(csvFilePath, content);

        Instances data = DatasetLoader.load(csvFilePath, "sentiment_label");

        assertThat(data).isNotNull();
        assertThat(data.numInstances()).isEqualTo(5);
        assertThat(data.classIndex()).isGreaterThanOrEqualTo(0);
        assertThat(data.classAttribute().name()).isEqualTo("sentiment_label");
    }

    @Test
    /**
     * Test loading a dataset missing the required class attribute.
     * This should throw an IllegalArgumentException.
     * @param tempDir Temporary directory for test files
     * @throws IOException
     */
    void testLoadMissingClassAttribute(@TempDir Path tempDir) throws IOException {
        Path csvFilePath = tempDir.resolve("test.csv");
        String content = """
                review_id,review_text
                1,"Great product"
                """;
        Files.writeString(csvFilePath, content);

        assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "sentiment_label"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing class attribute");
    }

    @Test
    /**
     * Test loading a dataset missing the not required class attribute.
     * This should not throw an exception, and the dataset should be loaded.
     * @param tempDir Temporary directory for test files
     * @throws IOException
     */
    void testLoadMissingNotRequiredClassAttribute(@TempDir Path tempDir) throws Exception {
        Path csvFilePath = tempDir.resolve("test.csv");
        String content = """
                review_text
                "Great product"
                "My Mistake"
                "Mid"
                """;
        Files.writeString(csvFilePath, content);

        Instances data = DatasetLoader.load(csvFilePath, "review_text");
        assertThat(data).isNotNull();
        assertThat(data.numInstances()).isEqualTo(3);
        assertThat(data.classIndex()).isGreaterThanOrEqualTo(0);
        assertThat(data.classAttribute().name()).isEqualTo("review_text");
    }

    @Test
    /**
     * Test loading a dataset that is ill-formatted but still contains
     * the class attribute. This should load successfully. For example,
     * if the table cell contains quotes, slashes, or commas, which are 
     * common in user-generated text reviews.
     * @param tempDir Temporary directory for test files
     * @throws IOException
     */
    void testLoadIllFormattedDataset(@TempDir Path tempDir) throws Exception {
        Path csvFilePath = tempDir.resolve("test.csv");
        String content = """
                review_text
                "Great / product"
                "My Mistake,"
                "Mid\'"
                "\"Unwanted\" product"
                "___garbage___"
                "'suprisingly' useless"
                "''suprisingly' useless"
                "'''''suprisingly' useless"
                "''\''suprisingly' useless"
                """;
        Files.writeString(csvFilePath, content);

        Instances data = DatasetLoader.load(csvFilePath, "review_text");
        assertThat(data).isNotNull();
        assertThat(data.numInstances()).isEqualTo(9);
        assertThat(data.classIndex()).isGreaterThanOrEqualTo(0);
        assertThat(data.classAttribute().name()).isEqualTo("review_text");
    }

    @Test
    /**
     * Test loading a non-existent file. This should throw an IOException.
     * @param tempDir
     */
    void testLoadNonExistentFile(@TempDir Path tempDir) {
        Path csvFile = tempDir.resolve("nonexistent.csv");

        assertThatThrownBy(() -> DatasetLoader.load(csvFile, "sentiment_label"))
                .isInstanceOf(IOException.class);
    }

    @Test
    /**
     * Test loading an empty dataset file. This should load successfully
     * but contains zero instances.
     * @param tempDir
     * @throws Exception
     */
    void testLoadEmptyDataset(@TempDir Path tempDir) throws Exception {
        Path csvFile = tempDir.resolve("empty.csv");
        String content = "review_id,review_text,sentiment_label\n";
        Files.writeString(csvFile, content);

        Instances data = DatasetLoader.load(csvFile, "sentiment_label");

        assertThat(data.numInstances()).isEqualTo(0);
        assertThat(data.classAttribute().name()).isEqualTo("sentiment_label");
    }
}
