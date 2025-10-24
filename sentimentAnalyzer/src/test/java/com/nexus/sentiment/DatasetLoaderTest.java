package com.nexus.sentiment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import weka.core.Instances;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class DatasetLoaderTest {

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
}
