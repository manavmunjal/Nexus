package com.nexus.sentiment;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import weka.core.Instances;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

import weka.core.Attribute;

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
              review_text,rating
              "Great / product",5
              "My Mistake,",1
              "Mid\'",3
              "\\"Unwanted\\" product",1
              "___garbage___",1
              "'suprisingly' useless",1
              "''suprisingly' useless",1
              "'''''suprisingly' useless",1
              "''\''suprisingly' useless",1
              """;
      Files.writeString(csvFilePath, content);

      Instances data = DatasetLoader.load(csvFilePath, "rating", "review_text");
      assertThat(data).isNotNull();
      assertThat(data.numInstances()).isEqualTo(9);
      assertThat(data.classIndex()).isGreaterThanOrEqualTo(0);
      assertThat(data.classAttribute().isNominal()).isTrue();
      assertThat(data.classAttribute().name()).isEqualTo("rating");
  }

  @Test
  /**
   * Test loading an empty file should throw an IllegalArgumentException
   * because the dataset contains no rows.
   */
  void testLoadEmptyFile(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("empty.csv");
      Files.writeString(csvFilePath, "");  // empty file

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "review_text", "rating"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Failed to load dataset from file"); // Only general message
  }

  @Test
  /**
   * Test missing class attribute should throw IllegalArgumentException
   */
  void testLoadMissingClassAttribute(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("missing_class.csv");
      Files.writeString(csvFilePath, "col1,col2\nval1,val2");

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "review_text", "review_text"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Missing class attribute");
  }

  @Test
  /**
   * Test when the text attribute is missing, should throw IllegalArgumentException.
   */
  void testLoadMissingTextAttribute(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("missing_text_attribute.csv");
      // Missing review_text column
      Files.writeString(csvFilePath, """
              rating,filler_column
              5,foo
              1,bar
              """);

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "rating", "review_text"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Missing text attribute: review_text");
  }

  @Test
  /**
   * Test numeric class triggers NumericToNominal filter.
   */
  void testLoadNumericClass(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("numeric_class.csv");
      Files.writeString(csvFilePath, """
              review_text,rating
              good,5
              bad,1
              """);

      Instances data = DatasetLoader.load(csvFilePath, "rating", "review_text");

      // After NumericToNominal, the class attribute should be nominal
      assertThat(data.classAttribute().isNominal()).isTrue();
  }

  @Test
  /**
   * Test that review_text values are cleaned of quotes and apostrophes
   */
  void testLoadReviewTextCleaned(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("clean_review.csv");
      Files.writeString(csvFilePath, """
              review_text,rating
              "Hello, world!",1
              "\'Test review\'",2
              "\\"Quoted\\" review",3
              """);

      Instances data = DatasetLoader.load(csvFilePath, "rating", "review_text");

      for (int i = 0; i < data.numInstances(); i++) {
          String val = data.instance(i).stringValue(data.attribute("review_text"));
          assertThat(val).doesNotContain("\"").doesNotContain("'");
      }
  }

  @Test
  void testLoadFileDoesNotExist(@TempDir Path tempDir) throws Exception {
      Path nonExistent = tempDir.resolve("nonexistent.csv");
      assertThatThrownBy(() -> DatasetLoader.load(nonExistent, "review_text", "rating"))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("CSV file does not exist");
  }

  @Test
  void testLoadMultiColumnHeader(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("multi.csv");
      // Multi-column header
      String content = "review_text,rating,company,product\n" +
                     "Good,5,CompanyA,ProductX\n" +
                     "Bad,1,CompanyB,ProductY";
      Files.writeString(csvFilePath, content);

      Instances data = DatasetLoader.load(csvFilePath, "rating", "review_text");

      // Should load successfully
      assertThat(data).isNotNull();
      assertThat(data.numInstances()).isEqualTo(2);
      assertThat(data.attribute("review_text")).isNotNull();
      assertThat(data.classAttribute().name()).isEqualTo("rating");
  }

  @Test
  /**
   * Test that loading a dataset with no review_text column
   * throws an IllegalArgumentException.
   */
  void testLoadReviewAttributeMissingThrows(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("no_review.csv");
      Files.writeString(csvFilePath, "rating\n1\n2");

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "rating", "review_text"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Dataset must contain at least text and class columns");
  }

  @Test
  void testLoadReviewTextCleanedQuotesAndApostrophes(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("review_text_quotes.csv");
      Files.writeString(csvFilePath, """
                                     review_text,rating
                                     "Great product",5
                                     'Bad review',1
                                     """);

      Instances data = DatasetLoader.load(csvFilePath, "rating", "review_text");

      for (int i = 0; i < data.numInstances(); i++) {
          String val = data.instance(i).stringValue(data.attribute("review_text"));
          assertThat(val).doesNotContain("\"").doesNotContain("'");
      }
  }

  @Test
  void testLoadSingleColumnOnlyClass(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("single_class.csv");
      Files.writeString(csvFilePath, "rating\n5\n1");

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "rating", "review_text"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Dataset must contain at least text and class columns"); // Specific message
  }

  @Test
  void testLoadNoRows(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("no_rows.csv");
      Files.writeString(csvFilePath, "review_text,rating\n");

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "rating", "review_text"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Dataset contains no rows"); // Specific message
  }

  @Test
  void testLoadTextAndClassSame(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("same_text_class.csv");
      Files.writeString(csvFilePath, "review_text,rating\nGood,1\nBad,2");

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "review_text", "review_text"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Text attribute and class attribute must be different"); // Specific message
  }

  @Test
  void testLoadTextOnlyEmpty(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("empty_text.csv");
      Files.writeString(csvFilePath, "review_text,rating\n?,5\n?,1");

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "rating", "review_text"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Text attribute contains no usable text"); // Specific message
  }

  @Test
  void testLoadMissingClassValues(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("missing_class_values.csv");
      Files.writeString(csvFilePath, "review_text,rating\nGood,?\nBad,?");

      assertThatThrownBy(() -> DatasetLoader.load(csvFilePath, "rating", "review_text"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Class attribute contains missing value at row"); // Specific message
  }

  @Test
  /**
   * Test when the class attribute is String, it should trigger StringToNominal conversion.
   */
  void testLoadClassAttributeString(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("non_nominal_class.csv");
      // Numeric class attribute
      Files.writeString(csvFilePath, """
              review_text,rating
              good,"5"
              bad,"1"
              """);

      Instances data = DatasetLoader.load(csvFilePath, "rating", "review_text");

      // After StringToNominal conversion, the class attribute should be nominal
      assertThat(data.classAttribute().isNominal()).isTrue();
  }

  @Test
  /**
   * Test when the text attribute is not of type string, it should trigger NominalToString conversion.
   */
  void testLoadTextAttributeNotString(@TempDir Path tempDir) throws Exception {
      Path csvFilePath = tempDir.resolve("non_string_text.csv");
      // Nominal text attribute
      Files.writeString(csvFilePath, """
              review_text,rating
              "good",5
              "bad",1
              """);

      Instances data = DatasetLoader.load(csvFilePath, "rating", "review_text");

      // After NominalToString conversion, the text attribute should be string
      assertThat(data.attribute("review_text").isString()).isTrue();
  }
}
