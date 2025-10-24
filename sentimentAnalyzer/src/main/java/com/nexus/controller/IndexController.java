package com.nexus.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the API index/welcome page.
 * Provides information about available API endpoints.
 */
@RestController
@RequestMapping("/api")
public class IndexController {
  
  /**
   * Returns a welcome message with a list of available API endpoints.
   *
   * @return a string containing the welcome message and endpoint descriptions
   */
  @GetMapping
  public String welcome() {
  try {
  return "Welcome to Sentiment Analysis API. Available endpoints:\n"
    + "- GET /api/sentiment/score?text=... - Get sentiment score\n"
    + "- POST /api/users - Create user\n"
    + "- POST /api/companies - Create company\n"
    + "- POST /api/products - Create product\n"
    + "- POST /api/products/{id}/reviews - Post review\n"
    + "- GET /api/products/{id}/reviews - Get product reviews\n"
    + "- PUT /api/products/{id}/reviews/{reviewId} - Update review";
  } catch (Exception e) {
  return "Error occurred while processing the request.";
  }
  }
}