package com.nexus.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public final class IndexController {

  private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

  /**
   * Returns a welcome message and lists available API endpoints.
   * This method is not designed for extension;
   * overriding may break request handling logic.
   *
   * @return ResponseEntity with welcome message or error
   */
  @GetMapping
  public ResponseEntity<String> welcome() {

    if (logger.isInfoEnabled()) {
      logger.info("Received request at /api (IndexController)");
    }

    try {
      String message = "Welcome to Sentiment Analysis API. Available endpoints:\n"
          + "- GET /api/sentiment/score?text=... - Get sentiment score\n"
          + "- POST /api/users - Create user\n"
          + "- POST /api/companies - Create company\n"
          + "- POST /api/products - Create product\n"
          + "- POST /api/products/{id}/reviews - Post review\n"
          + "- GET /api/products/{id}/reviews - Get product reviews\n"
          + "- PUT /api/products/{id}/reviews/{reviewId} - Update review";

      if (logger.isDebugEnabled()) {
        logger.debug("Successfully prepared API welcome message");
      }

      return ResponseEntity.ok(message);

    } catch (Exception e) {

      if (logger.isErrorEnabled()) {
        logger.error("Error while processing /api request: {}", e.getMessage(), e);
      }

      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error occurred while processing the request: " + e.getMessage());
    }
  }
}
