package com.nexus.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import com.nexus.sentiment.SentimentService;

class ProductControllerTest {

  private ProductRepository productRepository;
  private ReviewRepository reviewRepository;
  private UserRepository userRepository;
  private CompanyRepository companyRepository;
  private SentimentService sentimentService;
  private UserAuthService userAuthService;
  private ProductController controller;
  private Product product;

  /** Valid user ID for authenticated requests. */
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

    // Default: user authentication succeeds
    when(userAuthService.validateUser(VALID_USER_ID)).thenReturn(new AuthUser(VALID_USER_ID));
  }

  //  createProduct 

  @Test
  void createProduct_ShouldReturnCreatedProduct() {
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.createProduct(VALID_USER_ID, product);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(product, response.getBody());
    verify(productRepository, times(1)).save(product);
  }

  @Test
  void createProduct_ShouldReturnInternalServerError_OnDatabaseException() {
    when(productRepository.save(product)).thenThrow(new DataAccessException("DB down") {});

    ResponseEntity<?> response = controller.createProduct(VALID_USER_ID, product);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  assertNotNull(response.getBody());
  assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void createProduct_ShouldInitializeReviewIds_WhenNull() {
    product.setReviewIds(null);

    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.createProduct(VALID_USER_ID, product);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(product.getReviewIds());
    verify(productRepository, times(1)).save(product);
  }

  @Test
  void createProduct_ShouldAddProductToCompany_WhenCompanyExists() {
    product.setCompanyName("Acme");

    Company company = new Company();
    company.setProducts(null);

    when(productRepository.save(product)).thenReturn(product);
    when(companyRepository.findByName("Acme")).thenReturn(Optional.of(company));
    when(companyRepository.save(company)).thenReturn(company);

    ResponseEntity<?> response = controller.createProduct(VALID_USER_ID, product);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(company.getProducts());
    assertTrue(company.getProducts().contains("p1"));
  }

  @Test
  void createProduct_ShouldSkipCompanyUpdate_WhenCompanyNameBlank() {
    product.setCompanyName(" ");

    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.createProduct(VALID_USER_ID, product);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
  }

  @Test
  void createProduct_ShouldReturnUnauthorized_WhenUserDoesNotExist() {
    String invalidUserId = "invalid-user";
    when(userAuthService.validateUser(invalidUserId))
        .thenThrow(new IllegalStateException("User does not exist"));

    ResponseEntity<?> response = controller.createProduct(invalidUserId, product);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Authentication failed"));
  }

  //  getAllProducts 

  @Test
  void getAllProducts_ShouldReturnListOfProducts_WhenRepositoryReturnsData() {
    // Arrange
    Product product2 = new Product();
    product2.setId("p2");
    List<Product> products = List.of(product, product2);

    when(productRepository.findAll()).thenReturn(products);

    // Act
    ResponseEntity<?> response = controller.getAllProducts(VALID_USER_ID);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    List<Product> body = (List<Product>) response.getBody();
    assertEquals(2, body.size());
    assertTrue(body.contains(product));
    assertTrue(body.contains(product2));
  }

  @Test
  void getAllProducts_ShouldReturnInternalServerError_WhenRepositoryThrowsException() {
    // Arrange
    when(productRepository.findAll()).thenThrow(new RuntimeException("DB failure"));

    // Act
    ResponseEntity<?> response = controller.getAllProducts(VALID_USER_ID);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  void getAllProducts_ShouldReturnUnauthorized_WhenUserDoesNotExist() {
    String invalidUserId = "invalid-user";
    when(userAuthService.validateUser(invalidUserId))
        .thenThrow(new IllegalStateException("User does not exist"));

    ResponseEntity<?> response = controller.getAllProducts(invalidUserId);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  //  postReview 

  @Test
  void postReview_ShouldSetUserAndCalculateRating_WhenMissingRatingAndCommentPresent() {
    Review review = new Review();
    review.setComment("Great!");
    review.setRating(0);
    review.setUser(null);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(sentimentService.isTrained()).thenReturn(false);
    doNothing().when(sentimentService).trainModel(null, null, null);
    when(sentimentService.scoreFromText("Great!")).thenReturn(4.2);
    when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(4.2, ((Review) response.getBody()).getRating());
  }

  @Test
  void postReview_ShouldSkipRatingCalculation_WhenRatingProvided() {
    Review review = new Review();
    review.setRating(5);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.save(review)).thenReturn(review);
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(5, ((Review) response.getBody()).getRating());
  }

  @Test
  void postReview_ShouldReturnNotFound_WhenProductMissing() {
    Review review = new Review();
    when(productRepository.findById("p1")).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().toString().contains("Product not found"));
  }

  @Test
  void postReview_ShouldSetUserFromRepository_WhenUserExists() {
    Review review = new Review();
    review.setUser(new com.nexus.model.User());
    review.getUser().setId(null);
    review.getUser().setUsername("alice");
    review.setComment(null);
    review.setRating(0);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(userRepository.findByUsername("alice")).thenAnswer(i -> Optional.of(review.getUser()));
    when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(userRepository, times(1)).findByUsername("alice");
    verify(userRepository, never()).save(any());
  }

  @Test
  void postReview_ShouldSaveUser_WhenUserDoesNotExist() {
    com.nexus.model.User user = new com.nexus.model.User();
    user.setId(null);
    user.setUsername("bob");

    Review review = new Review();
    review.setUser(user);
    review.setComment(null);
    review.setRating(0);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());
    when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(userRepository, times(1)).save(user);
  }

  @Test
  void postReview_ShouldTrainSentiment_WhenNotTrainedAndCommentPresent() {
    Review review = new Review();
    review.setComment("Good product");
    review.setRating(0);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(sentimentService.isTrained()).thenReturn(false);
    doNothing().when(sentimentService).trainModel(null, null, null);
    when(sentimentService.scoreFromText("Good product")).thenReturn(4.5);
    when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(4.5, ((Review) response.getBody()).getRating());
    verify(sentimentService, times(1)).trainModel(null, null, null);
  }

  @Test
  void postReview_ShouldSkipTraining_WhenSentimentAlreadyTrained() {
    Review review = new Review();
    review.setComment("Nice");
    review.setRating(0);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(sentimentService.isTrained()).thenReturn(true);
    when(sentimentService.scoreFromText("Nice")).thenReturn(3.8);
    when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(3.8, ((Review) response.getBody()).getRating());
    verify(sentimentService, never()).trainModel(null, null, null);
  }

  @Test
  void postReview_ShouldSetRatingZero_WhenNoCommentAndRatingZero() {
    Review review = new Review();
    review.setRating(0);
    review.setComment(null);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(0, ((Review) response.getBody()).getRating());
  }

  @Test
  void postReview_ShouldInitializeReviewIds_WhenNull() {
    product.setReviewIds(null);

    Review review = new Review();
    review.setId("r1");

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.save(any())).thenReturn(review);
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(product.getReviewIds());
    assertTrue(product.getReviewIds().contains("r1"));
  }

  @Test
  void postReview_ShouldUpdateCompanyAverageRating_WhenCompanyNamePresent() {
    product.setCompanyName("Acme");

    Review review = new Review();
    review.setId("r1");
    review.setRating(5);

    Company company = new Company();
    company.setProducts(List.of("p1"));

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.save(review)).thenReturn(review);
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);
    when(companyRepository.findByProductsContaining("p1")).thenReturn(List.of(company));
    when(productRepository.findAllById(company.getProducts())).thenReturn(List.of(product));
    when(companyRepository.save(company)).thenReturn(company);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(5, company.getRating());
  }

  @Test
  void postReview_ShouldHandleUserWithBlankId() {
    com.nexus.model.User user = new com.nexus.model.User();
    user.setId(""); // blank id
    user.setUsername("charlie");

    Review review = new Review();
    review.setUser(user);
    review.setComment(null);
    review.setRating(0);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(userRepository.findByUsername("charlie")).thenReturn(Optional.empty());
    when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(userRepository, times(1)).save(user);
  }

  @Test
  void postReview_ShouldReturnInternalServerError_OnDataAccessException() {
    Review review = new Review();
    review.setRating(5);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.save(review)).thenThrow(new DataAccessException("DB error") {});

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void postReview_ShouldReturnInternalServerError_OnGenericException() {
    Review review = new Review();
    review.setRating(5);

    // Force a runtime exception on findById to hit generic catch
    when(productRepository.findById("p1")).thenThrow(new RuntimeException("Oops"));

    ResponseEntity<?> response = controller.postReview(VALID_USER_ID, "p1", review);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().toString().contains("Unexpected error"));
  }

  @Test
  void postReview_ShouldHandleMultipleUsersSeparately() {
      // First user
      User user1 = new User();
      user1.setId("");
      user1.setUsername("alice");
      Review review1 = new Review();
      review1.setUser(user1);
      review1.setComment("Great!");

      // Second user
      User user2 = new User();
      user2.setId("");
      user2.setUsername("bob");
      Review review2 = new Review();
      review2.setUser(user2);
      review2.setComment("Not bad.");

      when(productRepository.findById("p1")).thenReturn(Optional.of(product));
      when(reviewRepository.save(any())).thenAnswer(i -> {
          Review r = (Review) i.getArguments()[0];
          r.setId(r.getUser().getUsername()); // simulate DB-generated ID
          return r;
      });
      when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
      when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());
      when(sentimentService.isTrained()).thenReturn(true);
      when(sentimentService.scoreFromText(anyString())).thenReturn(4.0, 3.0); // different scores
      when(reviewRepository.findByIdIn(any())).thenReturn(List.of(review1, review2));

      // Post first review
      ResponseEntity<?> response1 = controller.postReview(VALID_USER_ID, "p1", review1);
      assertEquals(HttpStatus.CREATED, response1.getStatusCode());
      assertEquals("alice", ((Review) response1.getBody()).getId());
      assertTrue(product.getReviewIds().contains("alice"));

      // Post second review
      ResponseEntity<?> response2 = controller.postReview(VALID_USER_ID, "p1", review2);
      assertEquals(HttpStatus.CREATED, response2.getStatusCode());
      assertEquals("bob", ((Review) response2.getBody()).getId());
      assertTrue(product.getReviewIds().contains("bob"));

      // Verify that both users were saved separately
      verify(userRepository).save(user1);
      verify(userRepository).save(user2);

      // Verify that product's reviewIds contains both
      assertEquals(2, product.getReviewIds().size());
  }

  //  updateReview 

  @Test
  void updateReview_ShouldReturnInternalServerError_OnDataAccessException() {
    Review update = new Review();
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findById("r1")).thenReturn(Optional.of(new Review()));
    when(reviewRepository.save(any())).thenThrow(new DataAccessException("DB fail") {});

    ResponseEntity<?> response = controller.updateReview(VALID_USER_ID, "p1", "r1", update);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void updateReview_ShouldReturnNotFound_WhenProductMissing() {
    Review update = new Review();

    when(productRepository.findById("p1")).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.updateReview(VALID_USER_ID, "p1", "r1", update);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().toString().contains("Product not found"));
  }

  @Test
  void updateReview_ShouldReturnNotFound_WhenReviewMissing() {
    Review update = new Review();

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findById("r1")).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.updateReview(VALID_USER_ID, "p1", "r1", update);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().toString().contains("Review not found"));
  }

  @Test
  void updateReview_ShouldUpdateReviewWithoutUser() {
    product.setReviewIds(List.of("r1"));

    Review existing = new Review();
    existing.setId("r1");
    existing.setComment("Old");
    existing.setRating(3);

    Review update = new Review();
    update.setComment("New");
    update.setRating(5);
    update.setUser(null); // user null branch

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findById("r1")).thenReturn(Optional.of(existing));
    when(reviewRepository.findByIdIn(product.getReviewIds())).thenReturn(List.of(existing));
    when(reviewRepository.save(existing)).thenReturn(existing);
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.updateReview(VALID_USER_ID, "p1", "r1", update);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Review saved = (Review) response.getBody();
    assertNotNull(saved);
    assertEquals("New", saved.getComment());
    assertEquals(5, saved.getRating());
  }

  @Test
  void updateReview_ShouldUpdateReviewWithUser() {
    product.setReviewIds(List.of("r1"));

    Review existing = new Review();
    existing.setId("r1");

    Review update = new Review();
    update.setUser(new com.nexus.model.User());
    
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findById("r1")).thenReturn(Optional.of(existing));
    when(reviewRepository.findByIdIn(product.getReviewIds())).thenReturn(List.of(existing));
    when(reviewRepository.save(existing)).thenReturn(existing);
    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.updateReview(VALID_USER_ID, "p1", "r1", update);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Review saved = (Review) response.getBody();
    assertNotNull(saved);
    assertEquals(update.getUser(), saved.getUser());
  }

  @Test
  void updateReview_ShouldReturnInternalServerError_OnGenericException() {
    when(productRepository.findById("p1")).thenThrow(new RuntimeException("Oops"));

    Review update = new Review();
    ResponseEntity<?> response = controller.updateReview(VALID_USER_ID, "p1", "r1", update);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().toString().contains("Unexpected error"));
  }

  @Test
  void updateReview_ShouldUpdateCompanyAverageRating_WhenProductHasCompany() {
    // Setup product with company
    product.setCompanyName("AcmeCorp");
    product.setReviewIds(List.of("r1"));

    // Setup existing review
    Review existing = new Review();
    existing.setId("r1");
    existing.setRating(3);

    // Setup update review
    Review update = new Review();
    update.setRating(5);

    // Setup company
    Company company = new Company();
    company.setProducts(List.of("p1"));
    company.setRating(0);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findById("r1")).thenReturn(Optional.of(existing));
    when(reviewRepository.findByIdIn(product.getReviewIds())).thenReturn(List.of(existing));
    when(reviewRepository.save(existing)).thenReturn(existing);
    when(productRepository.save(product)).thenReturn(product);
    when(companyRepository.findByProductsContaining("p1")).thenReturn(List.of(company));
    when(productRepository.findAllById(company.getProducts())).thenReturn(List.of(product));
    when(companyRepository.save(company)).thenReturn(company);

    ResponseEntity<?> response = controller.updateReview(VALID_USER_ID, "p1", "r1", update);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Review saved = (Review) response.getBody();
    assertNotNull(saved);
    // Verify company rating updated
    assertEquals(product.getRating(), company.getRating());
    verify(companyRepository, times(1)).save(company);
  }

  //  getReviews 

  @Test
  void getReviews_ShouldReturnListOfReviews() {
    product.setReviewIds(List.of("r1"));

    Review review = new Review();
    review.setId("r1");

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findByIdIn(List.of("r1"))).thenReturn(List.of(review));

    ResponseEntity<?> response = controller.getReviews(VALID_USER_ID, "p1");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(List.of(review), response.getBody());
  }

  @Test
  void getReviews_ShouldReturnEmptyList_WhenReviewIdsNull() {
    product.setReviewIds(null);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<?> response = controller.getReviews(VALID_USER_ID, "p1");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(((List<?>) response.getBody()).isEmpty());
  }

  @Test
  void getReviews_ShouldReturnEmptyList_WhenReviewIdsEmpty() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<?> response = controller.getReviews(VALID_USER_ID, "p1");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(((List<?>) response.getBody()).isEmpty());
  }

  @Test
  void getReviews_ShouldReturnInternalServerError_OnGenericException() {
    when(productRepository.findById("p1")).thenThrow(new RuntimeException("Oops"));

    ResponseEntity<?> response = controller.getReviews(VALID_USER_ID, "p1");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().toString().contains("Unexpected error"));
  }

  @Test
  void getReviews_ShouldReturnReviews_WhenReviewIdsPopulated() {
    product.setReviewIds(List.of("r1", "r2"));

    Review r1 = new Review();
    r1.setId("r1");
    Review r2 = new Review();
    r2.setId("r2");

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findByIdIn(List.of("r1", "r2"))).thenReturn(List.of(r1, r2));

    ResponseEntity<?> response = controller.getReviews(VALID_USER_ID, "p1");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(List.of(r1, r2), response.getBody());
  }

  //  getAverageRating 

  @Test
  void getAverageRating_ShouldReturnOk_WhenProductExists() {
    product.setRating(4.5);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    ResponseEntity<?> response = controller.getAverageRating(VALID_USER_ID, "p1");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(4.5, response.getBody());
  }

  @Test
  void getAverageRating_ShouldReturnInternalServerError_OnException() {
    when(productRepository.findById("p1")).thenThrow(new RuntimeException("fail"));

    ResponseEntity<?> response = controller.getAverageRating(VALID_USER_ID, "p1");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Unexpected error"));
  }
}
