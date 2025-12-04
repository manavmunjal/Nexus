package com.nexus.sentiment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class SentimentServiceTest {

  private SentimentModelTrainer stubTrainer;
  private SentimentService sentimentService;
  private SentimentService blankService;
  private FilteredClassifier mockClassifier;
  private Instances mockInstances;
  private ScoreMapper mockMapper;

  private File tmpDir;

  @BeforeEach
  void setup() throws Exception {
    mockClassifier = mock(FilteredClassifier.class);
    mockMapper = mock(ScoreMapper.class);

    stubTrainer = mock(SentimentModelTrainer.class);
    when(stubTrainer.train(any(), anyString())).thenReturn(mockClassifier);

    // Prepare mock dataset
    mockInstances = buildMockDataset();

    // Temp folder for model files
    tmpDir = Files.createTempDirectory("tmp_saved_models").toFile();
    tmpDir.deleteOnExit();

    // Construct blank service, flexible to build on
    blankService = new SentimentService(stubTrainer, tmpDir.getAbsolutePath() + "/");

    // Construct service
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

    Instances data = new Instances("mock_data", attributes, 1);
    data.setClass(classAttr);

    Instance inst = new DenseInstance(2);
    inst.setValue(textAttr, "Sample review");
    inst.setValue(classAttr, "neutral");
    data.add(inst);

    return data;
  }

  // ---- scoreFromText ----

  @Test
  void testScoreFromText_highConfidencePositiveReturnsFive() throws Exception {
    double[] dist = {0.0, 0.0, 1.0};
    when(mockClassifier.distributionForInstance(any())).thenReturn(dist);

    try (MockedStatic<DatasetLoader> loaderMock = mockStatic(DatasetLoader.class);
        MockedStatic<ScoreMapper> mapperMock = mockStatic(ScoreMapper.class)) {

      loaderMock.when(() -> DatasetLoader.load(any(), any(), anyString())).thenReturn(mockInstances);
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

      loaderMock.when(() -> DatasetLoader.load(any(), any(), anyString())).thenReturn(mockInstances);
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
  void testScoreFromText_nullTextCausesException() {
    try (MockedStatic<DatasetLoader> loaderMock = mockStatic(DatasetLoader.class);
         MockedStatic<ScoreMapper> mapperMock = mockStatic(ScoreMapper.class)) {

      loaderMock.when(() -> DatasetLoader.load(any(), any(), anyString())).thenReturn(mockInstances);
      mapperMock.when(() -> ScoreMapper.fromAttribute(any())).thenReturn(mockMapper);

      // Train to avoid IllegalStateException
      sentimentService.trainModel(null, null, null);

      assertThrows(RuntimeException.class, () -> sentimentService.scoreFromText(null));
    }
  }

  // ---- saveModel ----

  @Test
  void saveModel_createsFiles() throws Exception {
    try (MockedStatic<SerializationHelper> serMock = mockStatic(SerializationHelper.class)) {
      serMock.when(() -> SerializationHelper.write(anyString(), any()))
              .thenAnswer(invocation -> null);

      sentimentService.saveModel();

      String classifierPath = new File(tmpDir, "sentiment_classifier.model").getAbsolutePath();
      String headerPath = new File(tmpDir, "sentiment_header.model").getAbsolutePath();
      String scoresPath = new File(tmpDir, "sentiment_scores.model").getAbsolutePath();

      serMock.verify(() -> SerializationHelper.write(classifierPath, mockClassifier));
      serMock.verify(() -> SerializationHelper.write(headerPath, mockInstances));
      serMock.verify(() -> SerializationHelper.write(scoresPath, mockMapper));
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

  // ---- loadModel ----

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
  void loadModel_success_loadsRealFiles() throws Exception {
    // Copy test model files from resources
    SentimentService realService = new SentimentService(stubTrainer, "src/test/resources/data/");

    // Load models from disk
    realService.loadModel();

    // Assertions: check objects are loaded and of correct type
    assertNotNull(realService.getClassifier());
    assertTrue(realService.getClassifier() instanceof FilteredClassifier);

    assertNotNull(realService.getTrainedHeader());
    assertTrue(realService.getTrainedHeader() instanceof Instances);

    assertNotNull(realService.getScoreMapper());
    assertTrue(realService.getScoreMapper() instanceof ScoreMapper);
  }

  // ---- ensureReady ----

  @Test
  void ensureReady_throwsIfNotTrained() throws Exception {
    Method m = SentimentService.class.getDeclaredMethod("ensureReady");
    m.setAccessible(true);

    InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> m.invoke(blankService));

    Throwable cause = ex.getCause();
    assertNotNull(cause);
    assertTrue(cause instanceof IllegalStateException);
    assertTrue(cause.getMessage().contains("Sentiment model not trained yet"));
  }

  @Test
  void ensureReady_passesIfTrained() throws Exception {
    Method m = SentimentService.class.getDeclaredMethod("ensureReady");
    m.setAccessible(true);

    // Call and assert no exception thrown
    assertDoesNotThrow(() -> m.invoke(sentimentService));
  }

  // ---- isTrained ----

  @Test
  void isTrainedShouldReturnFalseWhenAllFieldsNull() {
    assertFalse(blankService.isTrained(), "Expected isTrained() to be false when nothing is set");
  }

  @Test
  void isTrainedShouldReturnFalseWhenOnlyClassifierSet() {
    blankService.setClassifier(mockClassifier);
    assertFalse(blankService.isTrained(),
            "Expected isTrained() to be false when only classifier is set");
  }

  @Test
  void isTrainedShouldReturnFalseWhenClassifierAndMapperSet() {
    blankService.setClassifier(mockClassifier);
    blankService.setScoreMapper(mockMapper);

    assertFalse(blankService.isTrained(),
            "Expected isTrained() to be false when trainedHeader is still null");
  }

  @Test
  void isTrainedShouldReturnTrueWhenAllFieldsSet() {
    blankService.setClassifier(mockClassifier);
    blankService.setScoreMapper(mockMapper);
    blankService.setTrainedHeader(mockInstances);

    assertTrue(blankService.isTrained(),
            "Expected isTrained() to be true when classifier, scoreMapper, and trainedHeader are all non-null");
  }

  // ---- trainModel ----

  @Test
  void trainModel_withNullParameters_usesDefaults() throws Exception {

    // Static mocks needed for happy path
    try (MockedStatic<DatasetLoader> ld = mockStatic(DatasetLoader.class);
         MockedStatic<SentimentLabelConverter> conv = mockStatic(SentimentLabelConverter.class);
         MockedStatic<ScoreMapper> mapper = mockStatic(ScoreMapper.class)) {

      ld.when(() -> DatasetLoader.load(any(), any(), anyString())).thenReturn(mockInstances);
      conv.when(() -> SentimentLabelConverter.convertTo3Class(any(), any())).thenReturn(mockInstances);
      mapper.when(() -> ScoreMapper.fromAttribute(any())).thenReturn(mockMapper);

      blankService.trainModel(null, null, null);

      assertEquals(mockClassifier, blankService.getClassifier());
      assertEquals(mockMapper, blankService.getScoreMapper());
      assertEquals("review_text", blankService.getTrainedTextAttr());
    }
  }

  @Test
  void trainModel_withCustomParameters_callsLoaderWithProvidedValues() throws Exception {
    try (MockedStatic<DatasetLoader> ld = mockStatic(DatasetLoader.class);
         MockedStatic<SentimentLabelConverter> conv = mockStatic(SentimentLabelConverter.class);
         MockedStatic<ScoreMapper> mapper = mockStatic(ScoreMapper.class);
         MockedStatic<SerializationHelper> io = mockStatic(SerializationHelper.class)) {

        ld.when(() -> DatasetLoader.load(any(), any(), anyString())).thenReturn(mockInstances);
        conv.when(() -> SentimentLabelConverter.convertTo3Class(any(), any())).thenReturn(mockInstances);
        mapper.when(() -> ScoreMapper.fromAttribute(any())).thenReturn(mockMapper);

        io.when(() -> SerializationHelper.write(anyString(), any())).thenAnswer(inv -> null);

        String datasetPath = tmpDir.getAbsolutePath();
        String classAttr = "custom_label";
        String textAttr = "custom_text";

        blankService.trainModel(datasetPath, classAttr, textAttr);

        verify(stubTrainer).train(mockInstances, textAttr);
        assertEquals(textAttr, blankService.getTrainedTextAttr());
    }
  }

  @Test
  void trainModel_whenLoaderThrows_wrappedInRuntimeException() throws Exception {
    try (MockedStatic<DatasetLoader> loaderMock = mockStatic(DatasetLoader.class)) {
        loaderMock.when(() -> DatasetLoader.load(any(), any(), anyString()))
                  .thenThrow(new IOException("Failed to load"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> blankService.trainModel(null, null, null));

        assertTrue(ex.getMessage().contains("Failed to train sentiment model"));
        assertNotNull(ex.getCause());
        assertEquals(IOException.class, ex.getCause().getClass());
    }
  }

  @Test
  void trainModel_whenTrainerThrows_wrappedInRuntimeException() throws Exception {
    try (MockedStatic<DatasetLoader> loaderMock = mockStatic(DatasetLoader.class);
        MockedStatic<SentimentLabelConverter> converterMock = mockStatic(SentimentLabelConverter.class);
        MockedStatic<ScoreMapper> mapperMock = mockStatic(ScoreMapper.class)) {

        Instances dataMock = mockInstances;
        loaderMock.when(() -> DatasetLoader.load(any(), any(), anyString())).thenReturn(dataMock);
        converterMock.when(() -> SentimentLabelConverter.convertTo3Class(any(), any())).thenReturn(dataMock);
        mapperMock.when(() -> ScoreMapper.fromAttribute(any())).thenReturn(mockMapper);

        // Trainer throws
        SentimentModelTrainer failingTrainer = new SentimentModelTrainer() {
            @Override
            public FilteredClassifier train(Instances trainData, String textAttr) throws Exception {
                throw new Exception("Trainer failed");
            }
        };
        blankService = new SentimentService(failingTrainer, tmpDir.getAbsolutePath() + "/");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> blankService.trainModel(null, null, null));

        assertTrue(ex.getMessage().contains("Failed to train sentiment model"));
        assertEquals("Trainer failed", ex.getCause().getMessage());
    }
  }

  // ---- resolveDatasetPath ----

  @Test
  void resolveDatasetPath_existingFile_returnsPath() throws Exception {
      File tmpFile = Files.createTempFile("dataset_", ".csv").toFile();
      tmpFile.deleteOnExit();

      Method m = SentimentService.class.getDeclaredMethod("resolveDatasetPath", String.class);
      m.setAccessible(true);
      Path resolved = (Path) m.invoke(blankService, tmpFile.getAbsolutePath());

      assertEquals(tmpFile.toPath(), resolved);
  }

  @Test
  void resolveDatasetPath_inClasspath_returnsTempCopy() throws Exception {
    String resourcePath = "data/test_sample.csv";

    Method m = SentimentService.class.getDeclaredMethod("resolveDatasetPath", String.class);
    m.setAccessible(true);
    Path resolved = (Path) m.invoke(blankService, resourcePath);

    assertTrue(Files.exists(resolved));
    assertTrue(resolved.toString().endsWith(".csv"));

    String content = Files.readString(resolved);
    assertTrue(content.contains("dummy") || content.length() > 0);
  }

  @Test
  void resolveDatasetPath_missing_throws() throws Exception {
      Method m = SentimentService.class.getDeclaredMethod("resolveDatasetPath", String.class);
      m.setAccessible(true);

      String missingPath = "nonexistent.csv";

      Exception ex = assertThrows(Exception.class, () -> m.invoke(blankService, missingPath));

      Throwable cause = ex.getCause();
      assertTrue(cause instanceof IllegalArgumentException);
      assertTrue(cause.getMessage().contains("Dataset not found at path or classpath"));
  }
}