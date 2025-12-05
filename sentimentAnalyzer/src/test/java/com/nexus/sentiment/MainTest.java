package com.nexus.sentiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import weka.core.Instances;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainTest {

  private Main.Config config;

  @BeforeEach
  void setup() {
      // Initialize default config before each test
      config = new Main.Config();
      config.setDatasetPath("dummy/path.csv");
      config.setTextAttribute("review_text");
      config.setClassAttribute("sentiment_label");
      config.setTrainRatio(0.8);
      config.setSeed(42L);
      config.setEpsilon(1e-6);
      config.setSampleLimit(5);
      config.setShowHelp(false);
  }

  @Test
  void runAnalysisFileNotExistsThrows() {
      // Use a non-existent file path
      config.setDatasetPath("nonexistent/path/that/does/not/exist.csv");

      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
          Main.runAnalysis(config);
      });
      assertTrue(ex.getMessage().contains("Dataset not found"));
  }

  @Test
  void runAnalysisClassAttributeNullThrows() throws Exception {
      try (MockedStatic<DatasetLoader> datasetLoaderMock = mockStatic(DatasetLoader.class);
           MockedStatic<SentimentLabelConverter> converterMock = mockStatic(SentimentLabelConverter.class)) {

          // Use a path that exists in the test resources
          config.setDatasetPath("src/test/resources/data/test_reviews.csv");
          config.setClassAttribute("non_existent_attribute"); // Use a class attribute name that doesn't exist

          // Create a simple in-memory Instances object for testing
          ArrayList<weka.core.Attribute> atts = new ArrayList<>();
          atts.add(new weka.core.Attribute("some_attribute", (List<String>) null));
          Instances testInstances = new Instances("TestRelation", atts, 0);

          datasetLoaderMock.when(() -> DatasetLoader.load(any(Path.class), anyString(), anyString()))
                  .thenReturn(testInstances);
          converterMock.when(() -> SentimentLabelConverter.convertTo3Class(any(), anyString()))
                  .thenReturn(testInstances);

          IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
              Main.runAnalysis(config);
          });
          assertTrue(ex.getMessage().contains("Class attribute not found"));
      }
  }

  @Test
  void parseArgsSetsAllFieldsCorrectly() {
      String[] args = {
              "--dataset=path/to/data.csv",
              "--text-attr=my_text",
              "--class-attr=my_class",
              "--train-ratio=0.75",
              "--seed=12345",
              "--epsilon=0.0001",
              "--limit=7"
      };
      Main.Config parsedConfig = Main.parseArgs(args);

      assertEquals("path/to/data.csv", parsedConfig.getDatasetPath());
      assertEquals("my_text", parsedConfig.getTextAttribute());
      assertEquals("my_class", parsedConfig.getClassAttribute());
      assertEquals(0.75, parsedConfig.getTrainRatio());
      assertEquals(12345L, parsedConfig.getSeed());
      assertEquals(0.0001, parsedConfig.getEpsilon());
      assertEquals(7, parsedConfig.getSampleLimit());
      assertFalse(parsedConfig.isShowHelp());
  }

  @Test
  void parseArgsHelpFlagSetsShowHelp() {
      Main.Config parsedConfig = Main.parseArgs(new String[]{"--help"});
      assertTrue(parsedConfig.isShowHelp());

      parsedConfig = Main.parseArgs(new String[]{"-h"});
      assertTrue(parsedConfig.isShowHelp());
  }

  @Test
  void groupByGroupsCorrectly() {
      // Create real PredictionResult instances instead of mocks
      PredictionResult pr1 = new PredictionResult(
              "r1", "companyA", "prodA", "positive", "positive",
              0.9, new double[]{0.1, 0.2, 0.7}, Map.of()
      );

      PredictionResult pr2 = new PredictionResult(
              "r2", "companyB", "prodB", "negative", "negative",
              0.8, new double[]{0.7, 0.2, 0.1}, Map.of()
      );

      PredictionResult pr3 = new PredictionResult(
              "r3", "companyA", "prodA", "neutral", "positive",
              0.85, new double[]{0.1, 0.3, 0.6}, Map.of()
      );

      List<PredictionResult> list = List.of(pr1, pr2, pr3);

      Map<String, List<PredictionResult>> grouped = Main.groupBy(list, PredictionResult::product, "UNKNOWN");

      assertEquals(2, grouped.size());
      assertTrue(grouped.containsKey("prodA"));
      assertTrue(grouped.containsKey("prodB"));
      assertEquals(2, grouped.get("prodA").size());
      assertEquals(1, grouped.get("prodB").size());
  }

  @Test
  void groupByUsesFallbackWhenKeyIsNullOrBlank() {
      // Create real PredictionResult instances with null, empty, and blank product names
      PredictionResult pr1 = new PredictionResult(
              "r1", "companyA", null, "positive", "positive",
              0.9, new double[]{0.1, 0.2, 0.7}, Map.of()
      );

      PredictionResult pr2 = new PredictionResult(
              "r2", "companyB", "", "negative", "negative",
              0.8, new double[]{0.7, 0.2, 0.1}, Map.of()
      );

      PredictionResult pr3 = new PredictionResult(
              "r3", "companyC", "  ", "neutral", "positive",
              0.85, new double[]{0.1, 0.3, 0.6}, Map.of()
      );

      List<PredictionResult> list = List.of(pr1, pr2, pr3);

      Map<String, List<PredictionResult>> grouped = Main.groupBy(list, PredictionResult::product, "FALLBACK");

      assertEquals(1, grouped.size());
      assertTrue(grouped.containsKey("FALLBACK"));
      assertEquals(3, grouped.get("FALLBACK").size());
  }

  @Test
  void runAnalysisShowHelpOnlyPrintsHelp() throws Exception {
      Main.Config helpConfig = new Main.Config();
      helpConfig.setShowHelp(true);

      Main.runAnalysis(helpConfig);  // No exception expected
  }

  @Test
  void runAnalysisWithZeroOrNegativeSampleLimitPrintsNoPredictions() throws Exception {
      config.setDatasetPath("src/test/resources/data/test_reviews.csv");

      try (MockedStatic<DatasetLoader> datasetLoaderMock = mockStatic(DatasetLoader.class);
          MockedStatic<SentimentLabelConverter> converterMock = mockStatic(SentimentLabelConverter.class)) {

          // Attributes
          ArrayList<weka.core.Attribute> atts = new ArrayList<>();
          atts.add(new weka.core.Attribute("review_text", (List<String>) null));
          atts.add(new weka.core.Attribute("sentiment_label", List.of("positive", "neutral", "negative")));

          Instances testInstances = new Instances("TestRelation", atts, 5);

          for (int i = 0; i < 7; i++) {
              double[] vals = new double[testInstances.numAttributes()];
              vals[0] = testInstances.attribute(0).addStringValue("Review " + i);
              vals[1] = i % 3; // cycle through positive/neutral/negative
              testInstances.add(new weka.core.DenseInstance(1.0, vals));
          }

          datasetLoaderMock.when(() -> DatasetLoader.load(any(Path.class), anyString(), anyString()))
                  .thenReturn(testInstances);
          converterMock.when(() -> SentimentLabelConverter.convertTo3Class(any(), anyString()))
                  .thenReturn(testInstances);

          // Zero limit
          config.setSampleLimit(0);
          assertDoesNotThrow(() -> Main.runAnalysis(config));

          // Negative limit
          config.setSampleLimit(-5);
          assertDoesNotThrow(() -> Main.runAnalysis(config));
      }
  }

  @Test
  void runAnalysisWithSampleLimitExceedingPredictionCountPrintsAll() throws Exception {
      // Use a valid dataset path in test resources
      config.setDatasetPath("src/test/resources/data/test_reviews.csv");
      config.setSampleLimit(100); // larger than number of predictions

      try (MockedStatic<DatasetLoader> datasetLoaderMock = mockStatic(DatasetLoader.class);
          MockedStatic<SentimentLabelConverter> converterMock = mockStatic(SentimentLabelConverter.class)) {

          ArrayList<weka.core.Attribute> atts = new ArrayList<>();
          atts.add(new weka.core.Attribute("review_text", (List<String>) null));
          atts.add(new weka.core.Attribute("sentiment_label", List.of("positive", "neutral", "negative")));

          Instances testInstances = new Instances("TestRelation", atts, 5);
          testInstances.setClass(atts.get(1));

          for (int i = 0; i < 7; i++) {
              weka.core.Instance inst = new weka.core.DenseInstance(2);
              inst.setValue(atts.get(0), "dummy text " + i);
              inst.setValue(atts.get(1), "positive");
              testInstances.add(inst);
          }

          datasetLoaderMock.when(() -> DatasetLoader.load(any(Path.class), anyString(), anyString()))
                  .thenReturn(testInstances);
          converterMock.when(() -> SentimentLabelConverter.convertTo3Class(any(), anyString()))
                  .thenReturn(testInstances);

          // No exception should be thrown, sampleLimit > predictions.size() is clamped
          assertDoesNotThrow(() -> Main.runAnalysis(config));
      }
  }
}
