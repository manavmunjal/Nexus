package com.nexus.controller;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import com.nexus.model.Company;
import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.model.User;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;

class CompanyControllerTest {

  private CompanyRepository companyRepository;
  private CompanyController companyController;
  private ProductRepository productRepository;
  private ReviewRepository reviewRepository;
  private UserRepository userRepository;
  private UserAuthService userAuthService;

  private Company testCompany;

  /** Valid user ID for authenticated requests. */
  private static final String VALID_USER_ID = "test-user-123";

  @BeforeEach
  void setUp() {
    companyRepository = mock(CompanyRepository.class);
    productRepository = mock(ProductRepository.class);
    reviewRepository = mock(ReviewRepository.class);
    userRepository = mock(UserRepository.class);
    userAuthService = mock(UserAuthService.class);

    companyController = new CompanyController(companyRepository, productRepository,
        reviewRepository, userAuthService);

    // Default: user authentication succeeds
    when(userAuthService.validateUser(VALID_USER_ID)).thenReturn(new AuthUser(VALID_USER_ID));

    // Reusable company instance
    testCompany = new Company();
    testCompany.setId("c1");
    testCompany.setName("TestCompany");
    testCompany.setProducts(new ArrayList<>());
  }

  //  createCompany 

  @Test
  void createCompany_ShouldReturnCreated_WhenValidCompany() {
    // Arrange
    Company company = new Company();
    company.setName("OpenAI");

    when(companyRepository.save(company)).thenReturn(company);

    // Act
    ResponseEntity<?> response = companyController.createCompany(VALID_USER_ID, company);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(company, response.getBody());
    verify(companyRepository, times(1)).save(company);
  }

  @Test
  void createCompany_ShouldReturnBadRequest_WhenCompanyIsNull() {
    // Act
    ResponseEntity<?> response = companyController.createCompany(VALID_USER_ID, null);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Invalid company data"));
    verify(companyRepository, never()).save(any());
  }

  @Test
  void createCompany_ShouldReturnBadRequest_WhenCompanyNameIsEmpty() {
    // Arrange
    Company company = new Company();
    company.setName("  ");

    // Act
    ResponseEntity<?> response = companyController.createCompany(VALID_USER_ID, company);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Invalid company data"));
    verify(companyRepository, never()).save(any());
  }

  @Test
  void createCompany_ShouldReturnInternalServerError_WhenDatabaseErrorOccurs() {
    // Arrange
    Company company = new Company();
    company.setName("ErrorCorp");
    
    when(companyRepository.save(company))
        .thenThrow(new DataAccessException("DB down") {});

    // Act
    ResponseEntity<?> response = companyController.createCompany(VALID_USER_ID, company);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void createCompany_ShouldReturnInternalServerError_WhenUnexpectedExceptionOccurs() {
    // Arrange
    Company company = new Company();
    company.setName("FailCorp");

    when(companyRepository.save(company))
        .thenThrow(new RuntimeException("Unexpected failure"));

    // Act
    ResponseEntity<?> response = companyController.createCompany(VALID_USER_ID, company);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Unexpected error"));
  }

  //  getAllReviews 

  @Test
  void getAllReviews_ShouldReturnEmptyList_WhenCompanyHasNoProducts() {
    // Arrange
    testCompany.setProducts(new ArrayList<>());
    when(companyRepository.findById("c1")).thenReturn(Optional.of(testCompany));

    // Act
    ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(((List<?>) response.getBody()).isEmpty());
  }

  @Test
  void getAllReviews_ShouldReturnReviews_WhenProductsHaveReviews() {
    // Arrange
    testCompany.setProducts(List.of("p1", "p2"));

    Product product1 = new Product();
    product1.setId("p1");
    product1.setReviewIds(List.of("r1", "r2"));

    Product product2 = new Product();
    product2.setId("p2");
    product2.setReviewIds(List.of("r3"));

    Review review1 = new Review("r1", "Good", 5.0, new User("u1", "Alice", "alice@test.com"));
    Review review2 = new Review("r2", "Average", 3.0, new User("u2", "Bob", "bob@test.com"));
    Review review3 = new Review("r3", "Excellent", 4.5, new User("u3", "Charlie", "charlie@test.com"));

    when(companyRepository.findById("c1")).thenReturn(Optional.of(testCompany));
    when(productRepository.findAllById(List.of("p1", "p2"))).thenReturn(List.of(product1, product2));
    when(reviewRepository.findAllById(List.of("r1", "r2", "r3")))
        .thenReturn(List.of(review1, review2, review3));

    // Act
    ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(3, ((List<?>) response.getBody()).size());
  }

  @Test
  void getAllReviews_ShouldReturnInternalServerError_WhenDatabaseErrorOccurs() {
    // Arrange
    when(companyRepository.findById("c1")).thenThrow(new DataAccessException("DB down") {});

    // Act
    ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void getAllReviews_ShouldReturnInternalServerError_WhenUnexpectedExceptionOccurs() {
    // Arrange
    when(companyRepository.findById("c1")).thenThrow(new RuntimeException("Unexpected failure"));

    // Act
    ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Unexpected error"));
  }

  @Test
  void getAllReviews_ShouldThrowRuntime_WhenCompanyNotFound() {
    // Arrange
    when(companyRepository.findById("c1")).thenReturn(Optional.empty());

    // Act
    ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Company not found"));
  }

  @Test
  void getAllReviews_ShouldReturnEmptyList_WhenCompanyHasNoProducts_NullList() {
    // Arrange
    testCompany.setProducts(null);
    when(companyRepository.findById("c1")).thenReturn(Optional.of(testCompany));

    // Act
    ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(((List<?>) response.getBody()).isEmpty());
    verify(productRepository, never()).findAllById(any());
  }

  @Test
  void getAllReviews_ShouldReturnEmptyList_WhenProductsHaveNoReviews() {
    // Arrange
    testCompany.setProducts(List.of("p3", "p4"));

    Product p3 = new Product();
    p3.setId("p3");
    p3.setReviewIds(new ArrayList<>());

    Product p4 = new Product();
    p4.setId("p4");
    p4.setReviewIds(null);

    when(companyRepository.findById("c1")).thenReturn(Optional.of(testCompany));
    when(productRepository.findAllById(testCompany.getProducts())).thenReturn(List.of(p3, p4));

    // Act
    ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(((List<?>) response.getBody()).isEmpty());
  }

  @Test
  void getAllReviews_ShouldHandleReviewsWithNullUser() {
    // Arrange
    testCompany.setProducts(List.of("p1"));

    Product p1 = new Product();
    p1.setId("p1");
    p1.setReviewIds(List.of("r1"));

    Review review = new Review("r1", "Great", 5.0, null);

    when(companyRepository.findById("c1")).thenReturn(Optional.of(testCompany));
    when(productRepository.findAllById(List.of("p1"))).thenReturn(List.of(p1));
    when(reviewRepository.findAllById(List.of("r1"))).thenReturn(List.of(review));

    // Act
    ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    List<?> reviews = (List<?>) response.getBody();
    assertEquals(1, reviews.size());
    assertEquals(null, ((Review) reviews.get(0)).getUser());
  }

  @Test
  void getAllReviews_ShouldReturnMultipleUsersCorrectly() {
    // Arrange
    testCompany.setProducts(List.of("p1"));

    Product p1 = new Product();
    p1.setId("p1");
    p1.setReviewIds(List.of("r1", "r2"));

    Review r1 = new Review("r1", "Good", 4.0, new User("u1", "Alice", "alice@test.com"));
    Review r2 = new Review("r2", "Bad", 2.0, new User("u2", "Bob", "bob@test.com"));

    when(companyRepository.findById("c1")).thenReturn(Optional.of(testCompany));
    when(productRepository.findAllById(List.of("p1"))).thenReturn(List.of(p1));
    when(reviewRepository.findAllById(List.of("r1", "r2"))).thenReturn(List.of(r1, r2));

    // Act
    ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    List<?> reviews = (List<?>) response.getBody();
    assertEquals(2, reviews.size());
    assertTrue(reviews.stream().anyMatch(r -> ((Review) r).getUser().getUsername().equals("Alice")));
    assertTrue(reviews.stream().anyMatch(r -> ((Review) r).getUser().getUsername().equals("Bob")));
  }

  //  getAverageRating 

  @Test
  void getAverageRating_ShouldReturnRating_WhenCompanyExists() {
    // Arrange
    testCompany.setRating(4.5);
    when(companyRepository.findById("c1")).thenReturn(Optional.of(testCompany));

    // Act
    ResponseEntity<?> response = companyController.getAverageRating(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(4.5, response.getBody());
  }

  @Test
  void getAverageRating_ShouldReturnZero_WhenCompanyHasDefaultRating() {
    // Arrange
    testCompany.setRating(0.0);
    when(companyRepository.findById("c1")).thenReturn(Optional.of(testCompany));

    // Act
    ResponseEntity<?> response = companyController.getAverageRating(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(0.0, response.getBody());
  }

  @Test
  void getAverageRating_ShouldReturnInternalServerError_WhenCompanyNotFound() {
    // Arrange
    when(companyRepository.findById("c1")).thenReturn(Optional.empty());

    // Act
    ResponseEntity<?> response = companyController.getAverageRating(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Unexpected error occurred"));
  }

  @Test
  void getAverageRating_ShouldReturnInternalServerError_WhenDatabaseErrorOccurs() {
    // Arrange
    when(companyRepository.findById("c1"))
        .thenThrow(new DataAccessException("DB down") {});

    // Act
    ResponseEntity<?> response = companyController.getAverageRating(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void getAverageRating_ShouldReturnInternalServerError_WhenUnexpectedExceptionOccurs() {
    // Arrange
    when(companyRepository.findById("c1"))
        .thenThrow(new RuntimeException("Unexpected failure"));

    // Act
    ResponseEntity<?> response = companyController.getAverageRating(VALID_USER_ID, "c1");

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Unexpected error occurred"));
  }

  @Test
  void createCompany_ShouldReturnUnauthorized_WhenUserAuthFails() {
      // Arrange
      Company company = new Company();
      company.setName("AuthFailCorp");

      // Simulate authentication failure
      when(userAuthService.validateUser(VALID_USER_ID))
          .thenThrow(new IllegalStateException("User not authenticated"));

      // Act
      ResponseEntity<?> response = companyController.createCompany(VALID_USER_ID, company);

      // Assert
      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
      assertTrue(response.getBody().toString().contains("Authentication failed"));
      verify(companyRepository, never()).save(any());
  }

  @Test
  void createCompany_ShouldReturnBadRequest_WhenUserIdIsInvalid() {
      // Arrange
      Company company = new Company();
      company.setName("InvalidUserCorp");

      // Simulate invalid user ID
      when(userAuthService.validateUser(VALID_USER_ID))
          .thenThrow(new IllegalArgumentException("User ID is invalid"));

      // Act
      ResponseEntity<?> response = companyController.createCompany(VALID_USER_ID, company);

      // Assert
      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertTrue(response.getBody().toString().contains("Invalid user ID"));
      verify(companyRepository, never()).save(any());
  }

  @Test
  void getAllReviews_ShouldReturnUnauthorized_WhenUserAuthFails() {
      // Arrange
      when(userAuthService.validateUser(VALID_USER_ID))
          .thenThrow(new IllegalStateException("User not authenticated"));

      // Act
      ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

      // Assert
      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
      assertTrue(response.getBody().toString().contains("Authentication failed"));
  }

  @Test
  void getAllReviews_ShouldReturnBadRequest_WhenUserIdIsInvalid() {
      // Arrange
      when(userAuthService.validateUser(VALID_USER_ID))
          .thenThrow(new IllegalArgumentException("User ID is invalid"));

      // Act
      ResponseEntity<?> response = companyController.getAllReviews(VALID_USER_ID, "c1");

      // Assert
      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertTrue(response.getBody().toString().contains("Invalid user ID"));
  }

  @Test
  void getAverageRating_ShouldReturnUnauthorized_WhenUserAuthFails() {
      // Arrange
      when(userAuthService.validateUser(VALID_USER_ID))
          .thenThrow(new IllegalStateException("User not authenticated"));

      // Act
      ResponseEntity<?> response = companyController.getAverageRating(VALID_USER_ID, "c1");

      // Assert
      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
      assertTrue(response.getBody().toString().contains("Authentication failed"));
  }

  @Test
  void getAverageRating_ShouldReturnBadRequest_WhenUserIdIsInvalid() {
      // Arrange
      when(userAuthService.validateUser(VALID_USER_ID))
          .thenThrow(new IllegalArgumentException("User ID is invalid"));

      // Act
      ResponseEntity<?> response = companyController.getAverageRating(VALID_USER_ID, "c1");

      // Assert
      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertTrue(response.getBody().toString().contains("Invalid user ID"));
  }
}
