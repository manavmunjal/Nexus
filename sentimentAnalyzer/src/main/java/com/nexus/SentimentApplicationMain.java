package com.nexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
 public class SentimentApplicationMain {
  /**
   * Protected constructor to prevent instantiation.
   */
  protected SentimentApplicationMain() { }
  /**
   * Main entry point for the Sentiment Analysis Spring Boot application.
   *
   * @param args command-line arguments
   */
  public static void main(final String[] args) {
    SpringApplication.run(SentimentApplicationMain.class, args);
  }
 }
