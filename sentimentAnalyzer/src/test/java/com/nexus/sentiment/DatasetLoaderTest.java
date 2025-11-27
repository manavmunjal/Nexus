package com.nexus.sentiment;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import weka.core.Instances;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

import weka.core.Attribute;


import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;


class DatasetLoaderTest {

  @Test
  /**
   * Test loading a dataset that is ill-formatted but still contains
   * the class attribute. This should load successfully. For example,
   * if the table cell contains quotes, slashes, or commas, which are 
   * common in user-generated text reviews.
   * @param tempDir Temporary directory for test files
   * @throws Exception
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
   * Test loading an empty file should throw an IOException
   * because Weka cannot parse an empty CSV file.
   */
  void testLoadEmptyFile(@TempDir Path tempDir) throws Exception {
    Path csvFilePath = tempDir.resolve("empty.csv");
    Files.writeString(csvFilePath, "");  // empty file

    assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "review_text"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("No data in the file");
  }

  @Test
  /**
   * Test single-column CSV triggers temporary file creation branch
   */
  void testLoadSingleColumnCreatesTempFile(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("single.csv");
      Files.writeString(csvFilePath, """
              review_text
              Test review 1
              Test review 2
              """);

      Instances data = DatasetLoader.load(csvFilePath, "review_text");

      assertThat(data).isNotNull();
      assertThat(data.numInstances()).isEqualTo(2);
  }

  @Test
  /**
   * Test missing class attribute should throw IllegalArgumentException
   */
  void testLoadMissingClassAttribute(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("missing_class.csv");
      Files.writeString(csvFilePath, "col1,col2\nval1,val2");

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "review_text"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Missing class attribute");
  }

  @Test
  /**
   * Test non-nominal class triggers StringToNominal filter.
   */
  void testLoadNonNominalClass(@TempDir Path tempDir) throws Exception {
    Path csvFilePath = tempDir.resolve("numeric_class.csv");
    Files.writeString(csvFilePath, """
            review_text,rating
            good,"5"
            bad,"1"
            """);

    Instances data = DatasetLoader.load(csvFilePath, "rating");

    // After StringToNominal, the class attribute should be nominal
    assertThat(data.classAttribute().isNominal()).isTrue();
  }

  @Test
  void testLoadReviewTextWithEmptyValue(@TempDir Path tempDir) throws Exception {
    Path csvFilePath = tempDir.resolve("review_text_empty.csv");
    Files.writeString(csvFilePath, """
            review_text,rating
            ,5
            """);

    Instances data = DatasetLoader.load(csvFilePath, "rating");

    String val = data.instance(0).stringValue(data.attribute("review_text"));
    // weka represents missing strings as ?
    assertThat(val).isEqualTo("?");
  }

  @Test
  /**
   * Test that review_text values are cleaned of quotes and apostrophes
   */
  void testLoadReviewTextCleaned(@TempDir Path tempDir) throws Exception {
    Path csvFilePath = tempDir.resolve("clean_review.csv");
    Files.writeString(csvFilePath, """
            review_text
            "Hello, world!"
            "'Test review'"
            "\"Quoted\" review"
            """);

    Instances data = DatasetLoader.load(csvFilePath, "review_text");

    for (int i = 0; i < data.numInstances(); i++) {
        String val = data.instance(i).stringValue(data.attribute("review_text"));
        assertThat(val).doesNotContain("\"").doesNotContain("'");
    }
  }

  @Test
  void testLoadFileDoesNotExist(@TempDir Path tempDir) throws Exception {
    Path nonExistent = tempDir.resolve("nonexistent.csv");
    assertThatThrownBy(() -> DatasetLoader.load(nonExistent, "review_text"))
        .isInstanceOf(Exception.class);
  }

  @Test
  void testLoadMultiColumnHeaderDoesNotCreateTemp(@TempDir Path tempDir) throws Exception {
    Path csvFilePath = tempDir.resolve("multi.csv");
    // Multi-column header
    String content = "review_text,rating\nGood,5\nBad,1";
    Files.writeString(csvFilePath, content);

    Instances data = DatasetLoader.load(csvFilePath, "rating");

    // Should load successfully
    assertThat(data).isNotNull();
    assertThat(data.numInstances()).isEqualTo(2);
    assertThat(data.attribute("review_text")).isNotNull();
    assertThat(data.classAttribute().name()).isEqualTo("rating");
  }


  @Test
  void testLoadNumericClassAttributeTriggersNumericToNominal(@TempDir Path tempDir) throws Exception {
    Path csvFilePath = tempDir.resolve("numeric_class.csv");
    Files.writeString(csvFilePath, "review_text,rating\nGood,1\nBad,2");

    Instances data = DatasetLoader.load(csvFilePath, "rating");

    assertThat(data.classAttribute().isNominal()).isTrue();
  }

  @Test
  void testLoadReviewAttributeNullSkipsNominalToString(@TempDir Path tempDir) throws Exception {
    Path csvFilePath = tempDir.resolve("no_review.csv");
    Files.writeString(csvFilePath, "rating\n1\n2");

    Instances data = DatasetLoader.load(csvFilePath, "rating");

    assertThat(data.attribute("review_text")).isNull();
  }

  @Test
  void testLoadReviewTextCleanedQuotesAndApostrophes(@TempDir Path tempDir) throws Exception {
    Path csvFilePath = tempDir.resolve("review_text_quotes.csv");
    Files.writeString(csvFilePath, """
                                   review_text,rating
                                   "Great product",5
                                   'Bad review',1
                                   """);

    Instances data = DatasetLoader.load(csvFilePath, "rating");

    // Check that quotes and apostrophes are removed
    Attribute reviewAttr = data.attribute("review_text");
    for (int i = 0; i < data.numInstances(); i++) {
        String val = data.instance(i).stringValue(reviewAttr);
        assertThat(val).doesNotContain("\"").doesNotContain("'");
    }
  }
}