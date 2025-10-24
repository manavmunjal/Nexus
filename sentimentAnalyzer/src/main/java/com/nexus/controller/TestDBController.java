package com.nexus.controller;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for testing MongoDB database connectivity.
 * Provides a simple endpoint to verify database connection status.
 */
@RestController
@RequestMapping("/test")
public class TestDBController {
  private final MongoTemplate mongoTemplate;

  /**
   * Constructs a TestDBController with the specified MongoTemplate.
   *
   * @param mongoTemplate the MongoDB template for database operations
   */
  public TestDBController(MongoTemplate mongoTemplate) {
  this.mongoTemplate = mongoTemplate;
  }

  /**
   * Tests the MongoDB database connection.
   *
   * @return a success message if connected, or an error message if connection fails
   */
  @GetMapping
  public String testConnection() {
  try {
  mongoTemplate.getDb().listCollectionNames().first();
  return "MongoDB connection successful!";
  } catch (Exception e) {
  return "MongoDB connection failed: " + e.getMessage();
  }
  }
}

