package com.nexus.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
}
