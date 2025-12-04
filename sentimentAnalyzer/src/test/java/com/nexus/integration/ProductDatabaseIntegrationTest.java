package com.nexus.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.controller.ProductController;
import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import com.nexus.model.Product;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import com.nexus.sentiment.SentimentService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests verifying Database Interactions via Mocks.
 * This proves the code "talks" to the database correctly without needing a real
 * DB connection.
 */
@ExtendWith(MockitoExtension.class)
public class ProductDatabaseIntegrationTest {

  @Mock
  private ProductRepository productRepository;

  // Mocks for other dependencies required by the Controller
  @Mock
  private ReviewRepository reviewRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private CompanyRepository companyRepository;
  @Mock
  private SentimentService sentimentService;

  @Mock
  private UserAuthService userAuthService;

  @InjectMocks
  private ProductController productController;

  private static final String VALID_USER_ID = "test-user-123";

  @BeforeEach
  void setUp() {
    when(userAuthService.validateUser(VALID_USER_ID))
        .thenReturn(new AuthUser(VALID_USER_ID));
  }

  /**
   * Verifies that the 'save' method of the repository is actually called
   * when we create a product.
   */
  @Test
  public void testCreateProduct_CallsDatabaseSave() {
    // Arrange
    Product product = new Product();
    product.setName("Mock DB Test Product");

    when(productRepository.save(any(Product.class))).thenReturn(product);

    // Act
    productController.createProduct(VALID_USER_ID, product);

    // Assert: Verify the database 'save' method was called exactly once
    verify(productRepository, times(1)).save(product);
  }

  /**
   * Verifies that the application handles Database Exceptions gracefully.
   * We simulate a DB crash (DataAccessException) and ensure the app doesn't
   * crash.
   */
  @Test
  public void testDatabaseFailure_Returns500Error() {
    // Arrange
    Product product = new Product();

    // Simulate a Database Error (e.g., connection lost)
    when(productRepository.save(any(Product.class)))
        .thenThrow(new DataAccessException("Connection refused") {
        });

    // Act
    ResponseEntity<?> response = productController.createProduct(VALID_USER_ID, product);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    verify(productRepository, times(1)).save(product);
  }

  /**
   * Verifies that we correctly query the database by ID.
   */
  @Test
  public void testGetProduct_QueriesDatabaseById() {
    // Arrange
    String productId = "12345";
    Product mockProduct = new Product();
    mockProduct.setRating(5.0);

    when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

    // Act
    productController.getAverageRating(VALID_USER_ID, productId);

    // Assert: Verify we asked the repository for the specific ID
    verify(productRepository).findById(productId);
  }
}
