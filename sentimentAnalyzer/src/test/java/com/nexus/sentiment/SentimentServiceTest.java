package com.nexus.sentiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class SentimentServiceTest {

  private SentimentModelTrainer stubTrainer;
  private SentimentService sentimentService;
  private FilteredClassifier mockClassifier;
  private Instances mockInstances;
  private ScoreMapper mockMapper;

  private File tmpDir;

  @BeforeEach
  void setup() throws Exception {
    mockClassifier = mock(FilteredClassifier.class);
    mockMapper = mock(ScoreMapper.class);

    // Use a stub implementation instead of mocking
    stubTrainer = new SentimentModelTrainer() {
      @Override
      public FilteredClassifier train(
              final Instances trainData,
              final String textAttributeName) throws Exception {
        return mockClassifier;
      }
    };

    // Prepare mock dataset
    mockInstances = buildMockDataset();

    // Temp folder for model files
    tmpDir = Files.createTempDirectory("tmp_saved_models").toFile();
    tmpDir.deleteOnExit();

    // Construct service with injected temp folder
    sentimentService = new SentimentService(stubTrainer, tmpDir.getAbsolutePath() + "/");

    // Inject mocks for scoring
    sentimentService.setClassifier(mockClassifier);
    sentimentService.setTrainedHeader(mockInstances);
    sentimentService.setScoreMapper(mockMapper);
  }

  @AfterEach
  void cleanup() throws IOException {
    Files.walk(tmpDir.toPath())
         .map(Path::toFile)
         .forEach(File::delete);
  }

  private Instances buildMockDataset() {
  ArrayList<Attribute> attributes = new ArrayList<>();
  Attribute textAttr = new Attribute("review_text", (ArrayList<String>) null);

  ArrayList<String> classValues = new ArrayList<>();
  classValues.add("negative");
  classValues.add("neutral");
  classValues.add("positive");
  Attribute classAttr = new Attribute("sentiment_label", classValues);

  attributes.add(textAttr);
  attributes.add(classAttr);

  Instances data = new Instances("mock_data", attributes, 0);
  data.setClass(classAttr);
  return data;
  }
  @Test
  void testScoreFromText_highConfidencePositiveReturnsFive() throws Exception {
    double[] dist = {0.0, 0.0, 1.0};
    when(mockClassifier.distributionForInstance(any())).thenReturn(dist);

    try (MockedStatic<DatasetLoader> loaderMock = mockStatic(DatasetLoader.class);
        MockedStatic<ScoreMapper> mapperMock = mockStatic(ScoreMapper.class)) {

      loaderMock.when(() -> DatasetLoader.load(any(), any())).thenReturn(mockInstances);
      mapperMock.when(() -> ScoreMapper.fromAttribute(any())).thenReturn(mockMapper);

      when(mockMapper.scoreFor("negative")).thenReturn(-1.0);
      when(mockMapper.scoreFor("neutral")).thenReturn(0.0);
      when(mockMapper.scoreFor("positive")).thenReturn(1.0);

      // Train once before scoring
      sentimentService.trainModel(null, null, null);

      double score = sentimentService.scoreFromText("Amazing quality!");
      assertEquals(1.0, score, 0.1);
    }
  }

  @Test
  void testScoreFromText_highConfidenceNegativeReturnsZero() throws Exception {
    double[] dist = {1.0, 0.0, 0.0};
    when(mockClassifier.distributionForInstance(any())).thenReturn(dist);

    try (MockedStatic<DatasetLoader> loaderMock = mockStatic(DatasetLoader.class);
        MockedStatic<ScoreMapper> mapperMock = mockStatic(ScoreMapper.class)) {

      loaderMock.when(() -> DatasetLoader.load(any(), any())).thenReturn(mockInstances);
      mapperMock.when(() -> ScoreMapper.fromAttribute(any())).thenReturn(mockMapper);

      when(mockMapper.scoreFor("negative")).thenReturn(-1.0);
      when(mockMapper.scoreFor("neutral")).thenReturn(0.0);
      when(mockMapper.scoreFor("positive")).thenReturn(1.0);

      // Train once before scoring
      sentimentService.trainModel(null, null, null);

      double score = sentimentService.scoreFromText("Terrible service!");
      assertEquals(-1, score, 0.1);
    }
  }

  @Test
  void testScoreFromText_throwsOnNullInput() {
  try (MockedStatic<DatasetLoader> loaderMock = mockStatic(DatasetLoader.class);
       MockedStatic<ScoreMapper> mapperMock = mockStatic(ScoreMapper.class)) {

    loaderMock.when(() -> DatasetLoader.load(any(), any())).thenReturn(mockInstances);
    mapperMock.when(() -> ScoreMapper.fromAttribute(any())).thenReturn(mockMapper);

    // Train to avoid IllegalStateException
    sentimentService.trainModel(null, null, null);

    assertThrows(RuntimeException.class, () -> sentimentService.scoreFromText(null));
  }
  }

  @Test
  void saveModel_createsFiles() throws Exception {
    try (MockedStatic<SerializationHelper> serMock = mockStatic(SerializationHelper.class)) {

      serMock.when(() -> SerializationHelper.write(anyString(), any()))
             .thenAnswer(invocation -> null); // do nothing when write is called

      sentimentService.saveModel(); // save model

      // Check that it tries to write the correct content to the correct file
      serMock.verify(() -> SerializationHelper.write(
          new File(tmpDir, "sentiment_classifier.model").getAbsolutePath(),
          mockClassifier));

      serMock.verify(() -> SerializationHelper.write(
          new File(tmpDir, "sentiment_header.model").getAbsolutePath(),
          mockInstances));

      serMock.verify(() -> SerializationHelper.write(
          new File(tmpDir, "sentiment_scores.model").getAbsolutePath(),
          mockMapper));
    }
  }

  @Test
  void saveModel_writeFails_throwsRuntimeException() throws Exception {
    try (MockedStatic<SerializationHelper> serMock = mockStatic(SerializationHelper.class)) {
      // Simulate an issue with writing to files
      serMock.when(() -> SerializationHelper.write(anyString(), any()))
             .thenThrow(new RuntimeException("Disk full"));

      // Assert that runtime exception is thrown when we try to save
      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> sentimentService.saveModel());

      assertTrue(ex.getMessage().contains("Failed to save sentiment model"));
    }
  }

  @Test
  void loadModel_missingFiles_throwsRuntimeException() throws Exception {
    File emptyDir = Files.createTempDirectory("tmp_missing_models").toFile();
    emptyDir.deleteOnExit();

    // Construct a service pointing to an empty folder
    SentimentService emptyService = new SentimentService(stubTrainer, emptyDir.getAbsolutePath() + "/");

    // Assert that exception is thrown
    RuntimeException ex = assertThrows(RuntimeException.class, emptyService::loadModel);

    assertNotNull(ex.getCause());
    assertTrue(ex.getCause().getMessage().contains("Saved model files not found"));
  }

  @Test
  void loadModel_success_loadsFilesCorrectly() throws Exception {
    // Prepare files, since load model checks that they exist
    File classifierFile = new File(tmpDir, "sentiment_classifier.model");
    File headerFile = new File(tmpDir, "sentiment_header.model");
    File scoresFile = new File(tmpDir, "sentiment_scores.model");

    classifierFile.createNewFile();
    headerFile.createNewFile();
    scoresFile.createNewFile();

    try (MockedStatic<SerializationHelper> serMock = mockStatic(SerializationHelper.class)) {
        // Mock output of reading the files
        serMock.when(() -> SerializationHelper.read(classifierFile.getAbsolutePath()))
               .thenReturn(mockClassifier);
        serMock.when(() -> SerializationHelper.read(headerFile.getAbsolutePath()))
               .thenReturn(mockInstances);
        serMock.when(() -> SerializationHelper.read(scoresFile.getAbsolutePath()))
               .thenReturn(mockMapper);

        // Call loadModel
        sentimentService.loadModel();

        // Verify that the objects are correctly loaded
        assertEquals(mockClassifier, sentimentService.getClassifier());
        assertEquals(mockInstances, sentimentService.getTrainedHeader());
        assertEquals(mockMapper, sentimentService.getScoreMapper());
    }
  }
}