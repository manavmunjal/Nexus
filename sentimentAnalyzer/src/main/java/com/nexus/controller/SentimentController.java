package com.nexus.controller;

import com.nexus.sentiment.SentimentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for sentiment analysis operations.
 * Provides endpoints for analyzing text sentiment.
 */
@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {
  private final SentimentService sentimentService;

  /**
   * Constructs a SentimentController with the specified SentimentService.
   *
   * @param sentimentService the service for sentiment analysis operations
   */
  public SentimentController(SentimentService sentimentService) {
  this.sentimentService = sentimentService;
  }

  /**
   * Calculates a sentiment score for the provided text.
   *
   * @param text the text to analyze
   * @return the sentiment score, or 0.0 if an error occurs
   */
  @GetMapping("/score")
  public double score(@RequestParam("text") String text) {
  try {
  return sentimentService.scoreFromText(text);
  } catch (Exception e) {
  return 0.0;
  }
  }
}
