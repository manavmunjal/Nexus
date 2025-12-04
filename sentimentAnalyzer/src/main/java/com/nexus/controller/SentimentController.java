package com.nexus.controller;

import com.nexus.auth.service.UserAuthService;
import com.nexus.sentiment.SentimentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * REST controller providing endpoints for sentiment analysis operations.
 * Handles scoring and training of the sentiment model.
 */
@RestController
@RequestMapping("/api/sentiment")
public final class SentimentController {

  /**
   * The user ID that has admin privileges for model training.
   */
  private static final String ADMIN_USER_ID = "ADMIN";

  /**
   * Logger instance for SentimentController.
   */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SentimentController.class);

  /**
   * Service responsible for performing sentiment analysis operations.
   */
  private final SentimentService sentimentService;

  /**
   * Service for user authentication.
   */
  private final UserAuthService userAuthService;

  /**
   * Constructs a new SentimentController with the provided sentiment services.
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
   * Calculates the sentiment score for the provided text.
   * If the model is not trained, it attempts to load a saved model;
   * if unavailable, it returns an error instead of training a default model.
   *
   * @param userId the authenticated user ID (required header)
   * @param text   the input text to analyze
   * @return ResponseEntity containing the sentiment score or an error message
   */
  @GetMapping("/score")
  public ResponseEntity<?> score(
      @RequestHeader("X-User-Id") final String userId,
      @RequestParam("text") final String text) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Received sentiment score request for text length={}",
          text != null ? text.length() : 0);
    }

    try {
      // Validate user exists
      userAuthService.validateUser(userId);

      boolean trainedBefore = sentimentService.isTrained();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Model trained previously: {}", trainedBefore);
      }

      // Attempt to load model if not trained
      if (!trainedBefore) {
        try {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Attempting to load existing sentiment model...");
          }
          sentimentService.loadModel();

          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Model loaded successfully.");
          }
        } catch (Exception e) {
          if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("No saved model found. Cannot score text.", e);
          }

          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body("No trained sentiment model available.");
        }
      }

      double score = sentimentService.scoreFromText(text);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Sentiment score computed: {}", score);
      }

      return ResponseEntity.ok()
          .header("Model-Training", trainedBefore ? "performed" : "no")
          .body(score);

    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());

    } catch (IllegalArgumentException iae) {

      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Invalid input for sentiment scoring: {}",
        iae.getMessage());
      }

      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid input: " + iae.getMessage());

    } catch (Exception e) {

      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Unexpected error while calculating sentiment score", e);
      }

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error calculating sentiment score: " + e.getMessage());
    }
  }

  /**
   * Trains (or retrains) the sentiment model using the provided dataset
   * Only the ADMIN user is authorized to train the model.
   * and attribute names. If parameters are not supplied, default values
   * inside the sentiment service will be used.
   *
   * @param userId      the authenticated user ID (required header "ADMIN")
   * @param datasetPath optional file path to the training dataset
   * @param classAttr     optional name of the class label attribute
   * @param textAttr       optional name of the text attribute
   * @return ResponseEntity indicating success or failure
   */
  @PostMapping("/train")
  public ResponseEntity<?> train(
      @RequestHeader("X-User-Id") final String userId,
      @RequestParam(value = "datasetPath", required = false)
      final String datasetPath,
      @RequestParam(value = "classAttr", required = false)
      final String classAttr,
      @RequestParam(value = "textAttr", required = false)
      final String textAttr) {

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Training request received:"
      + " datasetPath={}, classAttr={}, textAttr={}",
          datasetPath, classAttr, textAttr);
    }

    try {
      // Validate user exists
      userAuthService.validateUser(userId);

      // Check if user has admin privileges
      if (!ADMIN_USER_ID.equals(userId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body("Access denied: Insufficient privileges to train the model");
      }

      sentimentService.trainModel(datasetPath, classAttr, textAttr);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Model training completed successfully.");
      }

      return ResponseEntity.ok("Model trained successfully");
    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());

    } catch (IllegalArgumentException iae) {

      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Invalid training parameters: {}", iae.getMessage());
      }

      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid training parameters: " + iae.getMessage());

    } catch (Exception e) {

      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Unexpected error during model training", e);
      }

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error training model: " + e.getMessage());
    }
  }
}
