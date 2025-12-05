package com.nexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Sentiment Analysis Spring Boot application.
 * This class is designed as a utility class and is not meant to be instantiated.
 */
@SpringBootApplication
public final class SentimentApplicationMain {

  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private SentimentApplicationMain() {
  }

  /**
   * Main entry point for the Sentiment Analysis Spring Boot application.
   *
   * @param args command-line arguments
   */
  public static void main(final String[] args) {
    SpringApplication.run(SentimentApplicationMain.class, args);
  }
}
