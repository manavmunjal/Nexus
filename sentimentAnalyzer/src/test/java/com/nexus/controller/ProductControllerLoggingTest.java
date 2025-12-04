package com.nexus.controller;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import com.nexus.model.Product;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import com.nexus.sentiment.SentimentService;

@ExtendWith(OutputCaptureExtension.class)
class ProductControllerLoggingTest {

  private ProductRepository productRepository;
  private ReviewRepository reviewRepository;
  private UserRepository userRepository;
  private CompanyRepository companyRepository;
  private SentimentService sentimentService;
  private UserAuthService userAuthService;
  private ProductController controller;
  private Product product;

  private static final String VALID_USER_ID = "test-user-123";

  @BeforeEach
  void setUp() {
    productRepository = mock(ProductRepository.class);
    reviewRepository = mock(ReviewRepository.class);
    userRepository = mock(UserRepository.class);
    companyRepository = mock(CompanyRepository.class);
    sentimentService = mock(SentimentService.class);
    userAuthService = mock(UserAuthService.class);

    controller = new ProductController(productRepository, reviewRepository, userRepository,
        companyRepository, sentimentService, userAuthService);

    product = new Product();
    product.setId("p1");
    product.setReviewIds(new ArrayList<>());

    when(userAuthService.validateUser(VALID_USER_ID)).thenReturn(new AuthUser(VALID_USER_ID));
  }

  @Test
  void createProduct_ShouldLogInfo_WhenRequestReceived(CapturedOutput output) {
    when(productRepository.save(product)).thenReturn(product);

    controller.createProduct(VALID_USER_ID, product);

    assertTrue(output.getOut().contains("Received request to create product"), "Should log info message");
  }

  @Test
  void createProduct_ShouldLogError_WhenDatabaseException(CapturedOutput output) {
    when(productRepository.save(product)).thenThrow(new DataAccessException("DB down") {
    });

    controller.createProduct(VALID_USER_ID, product);

    assertTrue(output.getOut().contains("Database error while saving product"), "Should log error message");
  }
}
