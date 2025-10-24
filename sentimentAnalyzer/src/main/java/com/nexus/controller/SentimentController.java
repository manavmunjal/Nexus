package com.nexus.controller;

import com.nexus.sentiment.SentimentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for sentiment analysis operations.
 * Provides endpoints for analyzing text sentiment.
 */
@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {

  private final SentimentService sentimentService;

  public SentimentController(SentimentService sentimentService) {
    this.sentimentService = sentimentService;
  }

  /**
   * Calculates a sentiment score for the provided text.
   *
   * @param text the text to analyze
   * @return ResponseEntity with the sentiment score or an error message
   */
  @GetMapping("/score")
  public ResponseEntity<?> score(@RequestParam("text") String text) {
    try {
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
}
