package com.nexus.controller;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import com.nexus.sentiment.SentimentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SentimentControllerTest {

  private SentimentService sentimentService;
  private UserAuthService userAuthService;
  private SentimentController controller;

  /** Valid user ID for authenticated requests. */
  private static final String VALID_USER_ID = "test-user-123";

  /** Admin user ID with training privileges. */
  private static final String ADMIN_USER_ID = "ADMIN";

  @BeforeEach
  void setup() {
    sentimentService = mock(SentimentService.class);
    userAuthService = mock(UserAuthService.class);
    controller = new SentimentController(sentimentService, userAuthService);

    // Default: user authentication succeeds for both regular and admin users
    when(userAuthService.validateUser(VALID_USER_ID)).thenReturn(new AuthUser(VALID_USER_ID));
    when(userAuthService.validateUser(ADMIN_USER_ID)).thenReturn(new AuthUser(ADMIN_USER_ID));
  }

  @Test
  void scoreShouldReturnBadRequestWhenNoSavedModelAvailable() {
    String text = "I love this product!";

    // Model not trained
    when(sentimentService.isTrained()).thenReturn(false);

    // Loading model fails
    doThrow(new RuntimeException("No saved model"))
        .when(sentimentService).loadModel();

    ResponseEntity<?> response = controller.score(VALID_USER_ID, text);

    assertEquals(400, response.getStatusCode().value());

    String body = String.valueOf(response.getBody());
    assertTrue(body.contains("No trained sentiment model available"));

    // Ensure no training or scoring happens
    verify(sentimentService, never()).trainModel(any(), any(), any());
    verify(sentimentService, never()).scoreFromText(any());
  }

  @Test
  void scoreShouldReturnBadRequestOnIllegalArgumentException() {
    String text = "";
    when(sentimentService.isTrained()).thenReturn(true);
    when(sentimentService.scoreFromText(text)).thenThrow(new IllegalArgumentException("Text cannot be empty"));

    ResponseEntity<?> response = controller.score(VALID_USER_ID, text);

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

    ResponseEntity<?> response = controller.score(VALID_USER_ID, text);

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

    ResponseEntity<?> response = controller.score(VALID_USER_ID, text);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(3.8, response.getBody());
    assertEquals("performed", response.getHeaders().getFirst("Model-Training"));
    verify(sentimentService, never()).trainModel(any(), any(), any());
    verify(sentimentService, times(1)).scoreFromText(text);
  }

  @Test
  void trainEndpointShouldInvokeServiceAndReturnOk_WhenAdminUser() {
    ResponseEntity<?> response = controller.train(ADMIN_USER_ID, "/tmp/data.csv", "label", "text");

    assertEquals(200, response.getStatusCode().value());
    String body = String.valueOf(response.getBody());
    assertTrue(body.contains("Model trained successfully"));
    verify(sentimentService, times(1)).trainModel("/tmp/data.csv", "label", "text");
  }

  @Test
  void trainEndpointShouldReturnForbidden_WhenNonAdminUser() {
    ResponseEntity<?> response = controller.train(VALID_USER_ID, "/tmp/data.csv", "label", "text");

    assertEquals(403, response.getStatusCode().value());
    String body = String.valueOf(response.getBody());
    assertTrue(body.contains("Access denied"));
    assertTrue(body.contains("Insufficient privileges"));
    verify(sentimentService, never()).trainModel(any(), any(), any());
  }

  @Test
  void scoreShouldReturnUnauthorized_WhenUserDoesNotExist() {
    String invalidUserId = "invalid-user";
    when(userAuthService.validateUser(invalidUserId))
        .thenThrow(new IllegalStateException("User does not exist"));

    ResponseEntity<?> response = controller.score(invalidUserId, "test text");

    assertEquals(401, response.getStatusCode().value());
    assertTrue(String.valueOf(response.getBody()).contains("Authentication failed"));
  }

  @Test
  void trainEndpointShouldReturnUnauthorized_WhenUserDoesNotExist() {
    String invalidUserId = "invalid-user";
    when(userAuthService.validateUser(invalidUserId))
        .thenThrow(new IllegalStateException("User does not exist"));

    ResponseEntity<?> response = controller.train(invalidUserId, null, null, null);

    assertEquals(401, response.getStatusCode().value());
    assertTrue(String.valueOf(response.getBody()).contains("Authentication failed"));
    verify(sentimentService, never()).trainModel(any(), any(), any());
  }

  @Test
  void scoreShouldWorkForMultipleUsers() {
    String textA = "great!";
    String textB = "bad!";

    // Authorize two distinct users
    when(userAuthService.validateUser("user123")).thenReturn(new AuthUser("user123"));

    when(sentimentService.isTrained()).thenReturn(true);
    when(sentimentService.scoreFromText(textA)).thenReturn(0.9);
    when(sentimentService.scoreFromText(textB)).thenReturn(-0.4);

    ResponseEntity<?> respAdmin = controller.score(ADMIN_USER_ID, textA);
    ResponseEntity<?> respUser = controller.score("user123", textB);

    assertEquals(200, respAdmin.getStatusCode().value());
    assertEquals(0.9, respAdmin.getBody());
    assertEquals(200, respUser.getStatusCode().value());
    assertEquals(-0.4, respUser.getBody());

    verify(userAuthService, times(1)).validateUser(ADMIN_USER_ID);
    verify(userAuthService, times(1)).validateUser("user123");
    verify(sentimentService, times(1)).scoreFromText(textA);
    verify(sentimentService, times(1)).scoreFromText(textB);
  }
}
