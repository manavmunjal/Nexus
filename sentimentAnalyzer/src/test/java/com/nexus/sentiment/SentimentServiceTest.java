package com.nexus.sentiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instances;

import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SentimentServiceTest {

  private SentimentModelTrainer mockTrainer;
  private SentimentService sentimentService;
  private FilteredClassifier mockClassifier;
  private Instances mockInstances;
  private ScoreMapper mockMapper;

  @BeforeEach
  void setup() throws Exception {
  mockTrainer = mock(SentimentModelTrainer.class);
  mockClassifier = mock(FilteredClassifier.class);
  mockMapper = mock(ScoreMapper.class);

  // Prepare mock dataset
  mockInstances = buildMockDataset();
  when(mockTrainer.train(any(), any())).thenReturn(mockClassifier);

  // Create service with injected trainer
  sentimentService = new SentimentService(mockTrainer);
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
  void testScoreFromText_returnsNormalizedScore() throws Exception {
  double[] dist = {0.2, 0.3, 0.5}; // leaning positive
  when(mockClassifier.distributionForInstance(any())).thenReturn(dist);

  try (MockedStatic<DatasetLoader> loaderMock = mockStatic(DatasetLoader.class);
       MockedStatic<ScoreMapper> mapperMock = mockStatic(ScoreMapper.class)) {

  loaderMock.when(() -> DatasetLoader.load(any(), any())).thenReturn(mockInstances);
  mapperMock.when(() -> ScoreMapper.fromAttribute(any())).thenReturn(mockMapper);

  when(mockMapper.scoreFor("negative")).thenReturn(-1.0);
  when(mockMapper.scoreFor("neutral")).thenReturn(0.0);
  when(mockMapper.scoreFor("positive")).thenReturn(1.0);

  // Preload dependencies
  sentimentService.scoreFromText("Nice experience!");

  double score = sentimentService.scoreFromText("Excellent product!");
  assertTrue(score >= 0 && score <= 5, "Normalized score should be between 0 and 5");
  }
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

  double score = sentimentService.scoreFromText("Amazing quality!");
  assertEquals(5.0, score, 0.1);
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

  double score = sentimentService.scoreFromText("Terrible service!");
  assertEquals(0.0, score, 0.1);
  }
  }

  @Test
  void testScoreFromText_throwsOnNullInput() {
  assertThrows(RuntimeException.class, () -> sentimentService.scoreFromText(null));
  }
}
