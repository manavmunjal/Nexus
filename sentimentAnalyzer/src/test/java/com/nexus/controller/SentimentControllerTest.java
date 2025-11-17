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
  void scoreShouldTrainOnDemandAndReturnScore() {
    String text = "I love this product!";
    when(sentimentService.isTrained()).thenReturn(false);
    when(sentimentService.scoreFromText(text)).thenReturn(4.2);

    ResponseEntity<?> response = controller.score(text);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(4.2, response.getBody());
    assertEquals("no", response.getHeaders().getFirst("Model-Training"));
    verify(sentimentService, times(1)).trainModel(null, null, null);
    verify(sentimentService, times(1)).scoreFromText(text);
  }

  @Test
  void scoreShouldReturnBadRequestOnIllegalArgumentException() {
    String text = "";
    when(sentimentService.isTrained()).thenReturn(true);
    when(sentimentService.scoreFromText(text)).thenThrow(new IllegalArgumentException("Text cannot be empty"));

    ResponseEntity<?> response = controller.score(text);

  assertEquals(400, response.getStatusCode().value());
  String body = String.valueOf(response.getBody());
  assertTrue(body.contains("Invalid input"));
    verify(sentimentService, times(1)).scoreFromText(text);
  }

  @Test
  void scoreShouldReturnInternalServerErrorOnOtherException() {
    String text = "Some text";
    when(sentimentService.isTrained()).thenReturn(true);
    when(sentimentService.scoreFromText(text)).thenThrow(new RuntimeException("Model not loaded"));

    ResponseEntity<?> response = controller.score(text);

  assertEquals(500, response.getStatusCode().value());
  String body = String.valueOf(response.getBody());
  assertTrue(body.contains("Error calculating sentiment score"));
    verify(sentimentService, times(1)).scoreFromText(text);
  }

  @Test
  void scoreShouldNotRetrainIfAlreadyTrained() {
    String text = "Great value";
    when(sentimentService.isTrained()).thenReturn(true);
    when(sentimentService.scoreFromText(text)).thenReturn(3.8);

    ResponseEntity<?> response = controller.score(text);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(3.8, response.getBody());
    assertEquals("performed", response.getHeaders().getFirst("Model-Training"));
    verify(sentimentService, never()).trainModel(any(), any(), any());
    verify(sentimentService, times(1)).scoreFromText(text);
  }

  @Test
  void trainEndpointShouldInvokeServiceAndReturnOk() {
    ResponseEntity<?> response = controller.train("/tmp/data.csv", "label", "text");

  assertEquals(200, response.getStatusCode().value());
  String body = String.valueOf(response.getBody());
  assertTrue(body.contains("Model trained successfully"));
    verify(sentimentService, times(1)).trainModel("/tmp/data.csv", "label", "text");
  }
}
