package com.nexus.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import com.nexus.sentiment.SentimentService;

@ExtendWith(OutputCaptureExtension.class)
class SentimentControllerLoggingTest {

  private SentimentService sentimentService;
  private UserAuthService userAuthService;
  private SentimentController controller;

  private static final String VALID_USER_ID = "test-user-123";

  @BeforeEach
  void setUp() {
    sentimentService = mock(SentimentService.class);
    userAuthService = mock(UserAuthService.class);
    controller = new SentimentController(sentimentService, userAuthService);

    when(userAuthService.validateUser(VALID_USER_ID)).thenReturn(new AuthUser(VALID_USER_ID));
  }

  @Test
  void score_ShouldLogInfo_WhenRequestReceived(CapturedOutput output) {
    when(sentimentService.isTrained()).thenReturn(true);
    when(sentimentService.scoreFromText("test text")).thenReturn(0.5);

    controller.score(VALID_USER_ID, "test text");

    assertTrue(output.getOut().contains("Received sentiment score request"), "Should log info message");
  }

  @Test
  void score_ShouldLogError_WhenExceptionOccurs(CapturedOutput output) {
    when(sentimentService.isTrained()).thenReturn(true);
    when(sentimentService.scoreFromText("test text")).thenThrow(new RuntimeException("Analysis failed"));

    controller.score(VALID_USER_ID, "test text");

    assertTrue(output.getOut().contains("Unexpected error while calculating sentiment score"),
        "Should log error message");
  }

  @Test
  void score_ShouldLogInfo_WhenModelNotTrainedAndLoadedSuccessfully(CapturedOutput output) throws Exception {
    when(sentimentService.isTrained()).thenReturn(false);
    doNothing().when(sentimentService).loadModel();
    when(sentimentService.scoreFromText("test text")).thenReturn(0.8);

    controller.score(VALID_USER_ID, "test text");

    String logs = output.getOut();
    assertTrue(logs.contains("Attempting to load existing sentiment model..."), "Should log attempt to load model");
    assertTrue(logs.contains("Model loaded successfully."), "Should log successful model load");
    assertTrue(logs.contains("Sentiment score computed: 0.8"), "Should log computed score");
  }

  @Test
  void score_ShouldLogWarn_WhenModelNotTrainedAndNoSavedModelFound(CapturedOutput output) throws Exception {
    when(sentimentService.isTrained()).thenReturn(false);
    doThrow(new RuntimeException("Model file missing")).when(sentimentService).loadModel();

    ResponseEntity<?> response = controller.score(VALID_USER_ID, "test text");

    assertEquals(400, response.getStatusCodeValue());
    assertTrue(output.getOut().contains("No saved model found. Cannot score text."), "Should log warning about missing model");
  }

  @Test
  void train_ShouldLogInfo_WhenTrainingRequestReceived(CapturedOutput output) throws Exception {
    when(userAuthService.validateUser("ADMIN")).thenReturn(new AuthUser("ADMIN"));
    doNothing().when(sentimentService).trainModel(any(), any(), any());

    controller.train("ADMIN", "dataset.csv", "sentiment", "review_text");

    String logs = output.getOut();
    assertTrue(logs.contains("Training request received: datasetPath=dataset.csv, classAttr=sentiment, textAttr=review_text"), "Should log training request info");
    assertTrue(logs.contains("Model training completed successfully."), "Should log successful training");
  }

  @Test
  void train_ShouldLogError_WhenIllegalArgumentExceptionThrown(CapturedOutput output) throws Exception {
    when(userAuthService.validateUser("ADMIN")).thenReturn(new AuthUser("ADMIN"));
    doThrow(new IllegalArgumentException("bad parameters")).when(sentimentService).trainModel(any(), any(), any());

    controller.train("ADMIN", "dataset.csv", "sentiment", "review_text");

    assertTrue(output.getOut().contains("Invalid training parameters: bad parameters"), "Should log invalid training parameters error");
  }

  @Test
  void train_ShouldLogError_WhenUnexpectedExceptionThrown(CapturedOutput output) throws Exception {
    when(userAuthService.validateUser("ADMIN")).thenReturn(new AuthUser("ADMIN"));
    doThrow(new RuntimeException("training failed")).when(sentimentService).trainModel(any(), any(), any());

    controller.train("ADMIN", "dataset.csv", "sentiment", "review_text");

    assertTrue(output.getOut().contains("Unexpected error during model training"), "Should log unexpected training error");
  }
}