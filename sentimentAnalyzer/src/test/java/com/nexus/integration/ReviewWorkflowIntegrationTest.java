package com.nexus.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import com.nexus.controller.ProductController;
import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.model.User;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import com.nexus.sentiment.SentimentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Integration tests for Review Workflow via {@link ProductController}.
 *
 * <p>
 * These tests verify the review posting workflow, including sentiment analysis
 * integration.
 */
@WebMvcTest(ProductController.class)
public class ReviewWorkflowIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ProductRepository productRepository;
  @MockBean
  private ReviewRepository reviewRepository;
  @MockBean
  private UserRepository userRepository;
  @MockBean
  private CompanyRepository companyRepository;
  @MockBean
  private SentimentService sentimentService;
  @MockBean
  private UserAuthService userAuthService;

  private ObjectMapper objectMapper;
  private Product product;
  private Review review;
  private static final String VALID_USER_ID = "test-user-123";

  /**
   * Sets up common test data and initializes the ObjectMapper before each test.
   */
  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();

    product = new Product();
    product.setId("123");
    product.setName("Test Product");
    product.setReviewIds(new ArrayList<>());

    User user = new User();
    user.setUsername("testuser");

    review = new Review();
    review.setId("rev1");
    review.setComment("Great product!");
    review.setRating(5);
    review.setUser(user);

    // default auth success
    when(userAuthService.validateUser(VALID_USER_ID))
      .thenReturn(new AuthUser(VALID_USER_ID));
  }

  /**
   * Test scenario: Successfully posting a review for a product.
   *
   * <p>
   * Mocks the repositories and sentiment service to verify the review creation
   * flow.
   *
   * @throws Exception if the MockMvc request fails
   */
  @Test
  public void testPostReview_Success() throws Exception {
    when(productRepository.findById("123")).thenReturn(Optional.of(product));
    when(reviewRepository.save(any(Review.class))).thenReturn(review);
    when(productRepository.save(any(Product.class))).thenReturn(product);

    mockMvc
        .perform(
            post("/api/products/123/reviews")
          .header("X-User-Id", VALID_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(review)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.comment").value("Great product!"))
        .andExpect(jsonPath("$.rating").value(5))
        .andExpect(jsonPath("$.user.username").value("testuser"));
  }

  /**
   * Test scenario: Posting a review for a non-existent product.
   *
   * <p>
   * Mocks the ProductRepository to return empty and verifies the 404 response.
   *
   * @throws Exception if the MockMvc request fails
   */
  @Test
  public void testPostReview_ProductNotFound() throws Exception {
    when(productRepository.findById("999")).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/api/products/999/reviews")
          .header("X-User-Id", VALID_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(review)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$").value("Product not found: 999"));
  }
}
