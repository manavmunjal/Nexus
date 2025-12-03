package com.nexus.controller;

import com.nexus.auth.service.UserAuthService;
import com.nexus.sentiment.SentimentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * REST controller for sentiment analysis operations.
 * Provides endpoints for analyzing text sentiment.
 */
@RestController
@RequestMapping("/api/sentiment")
public final class SentimentController {

  /**
   * The user ID that has admin privileges for model training.
   */
  private static final String ADMIN_USER_ID = "ADMIN";

  /**
   * Service for sentiment analysis operations.
   */
  private final SentimentService sentimentService;

  /**
   * Service for user authentication.
   */
  private final UserAuthService userAuthService;

  /**
   * Constructs a SentimentController with the given services.
   *
   * @param service         the sentiment analysis service
   * @param newUserAuthService the user authentication service
   */
  public SentimentController(final SentimentService service,
      final UserAuthService newUserAuthService) {
    this.sentimentService = service;
    this.userAuthService = newUserAuthService;
  }

  /**
   * Calculates a sentiment score for the provided text.
   * Not designed for extension.
   *
   * @param userId the authenticated user ID (required header)
   * @param text   the text to analyze
   * @return ResponseEntity with the sentiment score or an error message
   */
  @GetMapping("/score")
  public ResponseEntity<?> score(
      @RequestHeader("X-User-Id") final String userId,
      @RequestParam("text") final String text) {
      try {
          // Validate user exists
          userAuthService.validateUser(userId);

          boolean trainedBefore = sentimentService.isTrained();
          if (!trainedBefore) {
              try {
                  System.out.println("Loading saved model.");
                  sentimentService.loadModel();
              } catch (Exception e) {
                  // if load fails, train with defaults
                  System.out.println("No model saved - training.");
                  sentimentService.trainModel(null, null, null);
              }
          }

          double score = sentimentService.scoreFromText(text);
          return ResponseEntity.ok()
              .header("Model-Training", trainedBefore ? "performed" : "no")
              .body(score);
      } catch (IllegalStateException ise) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                  .body("Authentication failed: " + ise.getMessage());
      } catch (IllegalArgumentException iae) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body("Invalid input: " + iae.getMessage());
      } catch (Exception e) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body("Error calculating sentiment score: " + e.getMessage());
      }
  }

  /**
   * Triggers training (or retraining) of the sentiment model.
   * Only the ADMIN user is authorized to train the model.
   * All parameters are optional; defaults are used when omitted.
   *
   * @param userId      the authenticated user ID (required header "ADMIN")
   * @param datasetPath optional path to CSV dataset
   * @param classAttr   optional class attribute name
   * @param textAttr    optional text attribute name
   * @return HTTP 200 if training succeeds, otherwise appropriate error
   */
  @PostMapping("/train")
  public ResponseEntity<?> train(
      @RequestHeader("X-User-Id") final String userId,
      @RequestParam(
        value = "datasetPath", required = false) final String datasetPath,
      @RequestParam(
        value = "classAttr", required = false) final String classAttr,
      @RequestParam(
        value = "textAttr", required = false) final String textAttr
  ) {
    try {
      // Validate user exists
      userAuthService.validateUser(userId);

      // Check if user has admin privileges
      if (!ADMIN_USER_ID.equals(userId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body("Access denied: Insufficient privileges to train the model");
      }

      sentimentService.trainModel(datasetPath, classAttr, textAttr);
      return ResponseEntity.ok("Model trained successfully");
    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());
    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid training parameters: " + iae.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error training model: " + e.getMessage());
    }
  }
}
