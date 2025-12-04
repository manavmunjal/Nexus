package com.nexus.controller;

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

/**
 * REST controller providing endpoints for sentiment analysis operations.
 * Handles scoring and training of the sentiment model.
 */
@RestController
@RequestMapping("/api/sentiment")
public final class SentimentController {

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
   * Constructs a new SentimentController with the provided sentiment service.
   *
   * @param service the sentiment analysis service
   */
  public SentimentController(final SentimentService service) {
    this.sentimentService = service;
  }

  /**
   * Calculates the sentiment score for the provided text.
   * If the model is not trained, it attempts to load a saved model;
   * if unavailable, it triggers a default training.
   *
   * @param text the input text to analyze
   * @return ResponseEntity containing the sentiment score or an error message
   */
  @GetMapping("/score")
  public ResponseEntity<?> score(@RequestParam("text") final String text) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Received sentiment score request for text length={}",
          text != null ? text.length() : 0);
    }

    try {
      boolean trainedBefore = sentimentService.isTrained();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Model trained previously: {}", trainedBefore);
      }

      // Attempt to load or train model if needed
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
            LOGGER.warn("No saved model found. Triggering default training.", e);
          }

          sentimentService.trainModel(null, null, null);

          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Default model training completed.");
          }
        }
      }

      double score = sentimentService.scoreFromText(text);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Sentiment score computed: {}", score);
      }

      return ResponseEntity.ok()
          .header("Model-Training", trainedBefore ? "performed" : "no")
          .body(score);

    } catch (IllegalArgumentException iae) {

      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Invalid input for sentiment scoring: {}", iae.getMessage());
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
   * and attribute names. If parameters are not supplied, default values
   * inside the sentiment service will be used.
   *
   * @param datasetPath optional file path to the training dataset
   * @param classAttr   optional name of the class label attribute
   * @param textAttr    optional name of the text attribute
   * @return ResponseEntity indicating success or failure
   */
  @PostMapping("/train")
  public ResponseEntity<?> train(
      @RequestParam(value = "datasetPath", required = false)
      final String datasetPath,
      @RequestParam(value = "classAttr", required = false)
      final String classAttr,
      @RequestParam(value = "textAttr", required = false)
      final String textAttr) {

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Training request received: datasetPath={}, classAttr={}, textAttr={}",
          datasetPath, classAttr, textAttr);
    }

    try {
      sentimentService.trainModel(datasetPath, classAttr, textAttr);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Model training completed successfully.");
      }

      return ResponseEntity.ok("Model trained successfully");

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
