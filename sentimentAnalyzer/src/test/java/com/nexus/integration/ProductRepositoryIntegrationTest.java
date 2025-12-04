package com.nexus.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.controller.ProductController;
import com.nexus.model.Product;
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
 * Integration tests for {@link ProductController}.
 *
 * <p>
 * These tests verify the main endpoints of the controller, including creating
 * products and fetching
 * average ratings, covering both success and not-found scenarios.
 */
@WebMvcTest(ProductController.class)
public class ProductRepositoryIntegrationTest {

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

  private ObjectMapper objectMapper;
  private Product product;

  /**
   * Sets up common test data and initializes the ObjectMapper before each test.
   *
   * <p>
   * Creates a sample Product object to be used in the tests.
   */
  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();

    // Create a sample product
    product = new Product();
    product.setId("123");
    product.setName("Test Product");
    product.setRating(4.5);
    product.setReviewIds(new ArrayList<>());
  }

  /**
   * Test scenario: Successfully creating a product.
   *
   * <p>
   * Mocks the ProductRepository to return the product and verifies the API
   * response status and
   * content.
   *
   * @throws Exception if the MockMvc request fails
   */
  @Test
  public void testCreateProduct_Success() throws Exception {
    when(productRepository.save(any(Product.class))).thenReturn(product);

    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Product"))
        .andExpect(jsonPath("$.rating").value(4.5));
  }

  /**
   * Test scenario: Fetch average rating for a product that exists.
   *
   * <p>
   * Mocks the ProductRepository to return a valid product and verifies the API
   * response status and
   * content.
   *
   * @throws Exception if the MockMvc request fails
   */
  @Test
  public void testGetProductAverageRating_Success() throws Exception {
    when(productRepository.findById("123")).thenReturn(Optional.of(product));

    mockMvc
        .perform(get("/api/products/123/average-rating"))
        .andExpect(status().isOk())
        .andExpect(content().string("4.5"));
  }

  /**
   * Test scenario: Fetch average rating for a product that does not exist (404).
   *
   * <p>
   * Mocks the ProductRepository to return empty and verifies that the API returns
   * a 500 status with
   * the correct error message (as per controller implementation which throws
   * RuntimeException).
   *
   * @throws Exception if the MockMvc request fails
   */
  @Test
  public void testGetProductAverageRating_NotFound() throws Exception {
    when(productRepository.findById("123")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/products/123/average-rating"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string("Unexpected error occurred: Product not found"));
  }
}
