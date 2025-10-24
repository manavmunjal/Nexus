package com.nexus.controller;

import com.nexus.sentiment.SentimentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SentimentControllerTest {

  private SentimentService sentimentService;
  private SentimentController controller;

  @BeforeEach
  void setup() {
    sentimentService = mock(SentimentService.class);
    controller = new SentimentController(sentimentService);
  }

  @Test
  void scoreShouldReturnSentimentScore() {
    String text = "I love this product!";
    when(sentimentService.scoreFromText(text)).thenReturn(4.2);

    ResponseEntity<?> response = controller.score(text);

  assertEquals(200, response.getStatusCode().value());
    assertEquals(4.2, response.getBody());
    verify(sentimentService, times(1)).scoreFromText(text);
  }

  @Test
  void scoreShouldReturnBadRequestOnIllegalArgumentException() {
    String text = "";
    when(sentimentService.scoreFromText(text)).thenThrow(new IllegalArgumentException("Text cannot be empty"));

    ResponseEntity<?> response = controller.score(text);

  assertEquals(400, response.getStatusCode().value());
  assertNotNull(response.getBody());
  assertTrue(response.getBody().toString().contains("Invalid input"));
    verify(sentimentService, times(1)).scoreFromText(text);
  }

  @Test
  void scoreShouldReturnInternalServerErrorOnOtherException() {
    String text = "Some text";
    when(sentimentService.scoreFromText(text)).thenThrow(new RuntimeException("Model not loaded"));

    ResponseEntity<?> response = controller.score(text);

  assertEquals(500, response.getStatusCode().value());
  assertNotNull(response.getBody());
  assertTrue(response.getBody().toString().contains("Error calculating sentiment score"));
    verify(sentimentService, times(1)).scoreFromText(text);
  }
}
