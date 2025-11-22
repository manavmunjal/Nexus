package com.nexus.controller;

import com.nexus.sentiment.SentimentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST controller for sentiment analysis operations.
 * Provides endpoints for analyzing text sentiment.
 */
@RestController
@RequestMapping("/api/sentiment")
public final class SentimentController {

  /**
   * Service for sentiment analysis operations.
   */
  private final SentimentService sentimentService;

  /**
   * Constructs a SentimentController with the given service.
   *
   * @param service the sentiment analysis service
   */
  public SentimentController(final SentimentService service) {
    this.sentimentService = service;
  }

  /**
   * Calculates a sentiment score for the provided text.
   * Not designed for extension.
   *
   * @param text the text to analyze
   * @return ResponseEntity with the sentiment score or an error message
   */
  @GetMapping("/score")
  public ResponseEntity<?> score(@RequestParam("text") final String text) {
      try {
          if (!sentimentService.isTrained()) {
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
          return ResponseEntity.ok(score);
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
   * All parameters are optional; defaults are used when omitted.
   *
   * @param datasetPath optional path to CSV dataset
   * @param classAttr optional class attribute name
   * @param textAttr optional text attribute name
   * @return HTTP 200 if training succeeds, otherwise appropriate error
   */
  @PostMapping("/train")
  public ResponseEntity<?> train(
      @RequestParam(
        value = "datasetPath", required = false) final String datasetPath,
      @RequestParam(
        value = "classAttr", required = false) final String classAttr,
      @RequestParam(
        value = "textAttr", required = false) final String textAttr
  ) {
    try {
      sentimentService.trainModel(datasetPath, classAttr, textAttr);
      return ResponseEntity.ok("Model trained successfully");
    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid training parameters: " + iae.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error training model: " + e.getMessage());
    }
  }
}
