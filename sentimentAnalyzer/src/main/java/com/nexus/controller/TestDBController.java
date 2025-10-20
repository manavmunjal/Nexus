package com.nexus.controller;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestDBController {
  private final MongoTemplate mongoTemplate;

  public TestDBController(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

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

