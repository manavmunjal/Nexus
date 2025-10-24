package com.nexus.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class IndexControllerTest {

  @InjectMocks
  private IndexController indexController;

  @Test
  void welcome_ShouldReturnWelcomeMessage() {
      String result = String.valueOf(indexController.welcome());
      
      assertNotNull(result);
      assertTrue(result.contains("Welcome to Sentiment Analysis API"));
      assertTrue(result.contains("/api/sentiment/score"));
      assertTrue(result.contains("/api/users"));
      assertTrue(result.contains("/api/products"));
  }
}