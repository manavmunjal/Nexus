package com.nexus.integration;

import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.model.User;
import com.nexus.model.Company;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import com.nexus.repository.CompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete review workflow.
 * Tests end-to-end scenarios involving products, reviews, users, and companies.
 */
@DataMongoTest
class ReviewWorkflowIntegrationTest {

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CompanyRepository companyRepository;

  @BeforeEach
  void setUp() {
    // Clean up before each test
    reviewRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    // Clean up after each test
    reviewRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @Test
  void testCompleteReviewWorkflow() {
    // Step 1: Create a user
    User user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user = userRepository.save(user);

    // Step 2: Create a product
    Product product = new Product();
    product.setName("Test Product");
    product.setReviewIds(new ArrayList<>());
    product = productRepository.save(product);

    // Step 3: Create a review
    Review review = new Review();
    review.setUser(user);
    review.setComment("Great product!");
    review.setRating(5);
    review = reviewRepository.save(review);

    // Step 4: Link review to product
    product.getReviewIds().add(review.getId());
    product = productRepository.save(product);

    // Assert: Verify the complete workflow
    Product retrievedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertEquals(1, retrievedProduct.getReviewIds().size());

    Review retrievedReview = reviewRepository.findById(review.getId()).orElseThrow();
    assertEquals("Great product!", retrievedReview.getComment());
    assertEquals(5, retrievedReview.getRating());
    assertNotNull(retrievedReview.getUser());
    assertEquals("testuser", retrievedReview.getUser().getUsername());
  }

  @Test
  void testMultipleReviewsFromDifferentUsers() {
    // Arrange
    User user1 = new User();
    user1.setUsername("user1");
    user1 = userRepository.save(user1);

    User user2 = new User();
    user2.setUsername("user2");
    user2 = userRepository.save(user2);

    Product product = new Product();
    product.setName("Popular Product");
    product.setReviewIds(new ArrayList<>());
    product = productRepository.save(product);

    Review review1 = new Review();
    review1.setUser(user1);
    review1.setComment("Excellent!");
    review1.setRating(5);
    review1 = reviewRepository.save(review1);

    Review review2 = new Review();
    review2.setUser(user2);
    review2.setComment("Good value");
    review2.setRating(4);
    review2 = reviewRepository.save(review2);

    // Act
    product.getReviewIds().add(review1.getId());
    product.getReviewIds().add(review2.getId());
    product = productRepository.save(product);

    // Assert
    Product retrievedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertEquals(2, retrievedProduct.getReviewIds().size());

    List<Review> reviews = reviewRepository.findByIdIn(retrievedProduct.getReviewIds());
    assertEquals(2, reviews.size());

    // Verify different users
    List<String> usernames = reviews.stream()
        .map(r -> r.getUser().getUsername())
        .toList();
    assertTrue(usernames.contains("user1"));
    assertTrue(usernames.contains("user2"));
  }

  @Test
  void testProductRatingUpdateAfterReviews() {
    // Arrange
    Product product = new Product();
    product.setName("Product for Rating Test");
    product.setReviewIds(new ArrayList<>());
    product.setRating(0.0);
    product = productRepository.save(product);

    Review review1 = new Review();
    review1.setRating(5);
    review1 = reviewRepository.save(review1);

    Review review2 = new Review();
    review2.setRating(3);
    review2 = reviewRepository.save(review2);

    Review review3 = new Review();
    review3.setRating(4);
    review3 = reviewRepository.save(review3);

    // Act
    product.getReviewIds().add(review1.getId());
    product.getReviewIds().add(review2.getId());
    product.getReviewIds().add(review3.getId());

    List<Review> reviews = reviewRepository.findByIdIn(product.getReviewIds());
    double avgRating = reviews.stream()
        .mapToDouble(Review::getRating)
        .average()
        .orElse(0.0);
    product.setRating(avgRating);
    product = productRepository.save(product);

    // Assert
    Product retrievedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertEquals(4.0, retrievedProduct.getRating(), 0.01); // (5 + 3 + 4) / 3 = 4.0
  }

  @Test
  void testCompanyRatingUpdateThroughProductReviews() {
    // Arrange
    Company company = new Company();
    company.setName("Test Company");
    company.setProducts(new ArrayList<>());
    company = companyRepository.save(company);

    // Create two products
    Product product1 = new Product();
    product1.setName("Product 1");
    product1.setReviewIds(new ArrayList<>());
    product1 = productRepository.save(product1);

    Product product2 = new Product();
    product2.setName("Product 2");
    product2.setReviewIds(new ArrayList<>());
    product2 = productRepository.save(product2);

    // Add products to company
    company.getProducts().add(product1.getId());
    company.getProducts().add(product2.getId());
    company = companyRepository.save(company);

    // Add reviews to product 1
    Review review1 = new Review();
    review1.setRating(5);
    review1 = reviewRepository.save(review1);
    product1.getReviewIds().add(review1.getId());

    // Add reviews to product 2
    Review review2 = new Review();
    review2.setRating(3);
    review2 = reviewRepository.save(review2);
    product2.getReviewIds().add(review2.getId());

    // Calculate product ratings
    List<Review> reviews1 = reviewRepository.findByIdIn(product1.getReviewIds());
    product1.setRating(reviews1.stream().mapToDouble(Review::getRating).average().orElse(0.0));
    product1 = productRepository.save(product1);

    List<Review> reviews2 = reviewRepository.findByIdIn(product2.getReviewIds());
    product2.setRating(reviews2.stream().mapToDouble(Review::getRating).average().orElse(0.0));
    product2 = productRepository.save(product2);

    // Act: Calculate company rating
    List<Product> products = productRepository.findAllById(company.getProducts());
    double companyRating = products.stream()
        .mapToDouble(Product::getRating)
        .average()
        .orElse(0.0);
    company.setRating(companyRating);
    company = companyRepository.save(company);

    // Assert
    Company retrievedCompany = companyRepository.findById(company.getId()).orElseThrow();
    assertEquals(4.0, retrievedCompany.getRating(), 0.01); // (5 + 3) / 2 = 4.0
  }

  @Test
  void testFindReviewsByUser() {
    // Arrange
    User user = new User();
    user.setUsername("prolific_reviewer");
    user = userRepository.save(user);

    Review review1 = new Review();
    review1.setUser(user);
    review1.setComment("Review 1");
    reviewRepository.save(review1);

    Review review2 = new Review();
    review2.setUser(user);
    review2.setComment("Review 2");
    reviewRepository.save(review2);

    Review review3 = new Review();
    User otherUser = new User();
    otherUser.setUsername("other_user");
    otherUser = userRepository.save(otherUser);
    review3.setUser(otherUser);
    review3.setComment("Review 3");
    reviewRepository.save(review3);

    // Act
    List<Review> allReviews = reviewRepository.findAll();

    // Assert
    assertEquals(3, allReviews.size());

    long userReviewCount = allReviews.stream()
        .filter(r -> r.getUser() != null && "prolific_reviewer".equals(r.getUser().getUsername()))
        .count();
    assertEquals(2, userReviewCount);
  }

  @Test
  void testReviewUpdate() {
    // Arrange
    Review review = new Review();
    review.setComment("Original comment");
    review.setRating(3);
    review = reviewRepository.save(review);
    String reviewId = review.getId();

    // Act
    Review retrievedReview = reviewRepository.findById(reviewId).orElseThrow();
    retrievedReview.setComment("Updated comment");
    retrievedReview.setRating(5);
    reviewRepository.save(retrievedReview);

    // Assert
    Review updatedReview = reviewRepository.findById(reviewId).orElseThrow();
    assertEquals("Updated comment", updatedReview.getComment());
    assertEquals(5, updatedReview.getRating());
  }

  @Test
  void testDeleteReview() {
    // Arrange
    Product product = new Product();
    product.setName("Product with Deletable Review");
    product.setReviewIds(new ArrayList<>());
    product = productRepository.save(product);

    Review review = new Review();
    review.setComment("To be deleted");
    review = reviewRepository.save(review);

    product.getReviewIds().add(review.getId());
    product = productRepository.save(product);

    // Act
    reviewRepository.deleteById(review.getId());
    product.getReviewIds().remove(review.getId());
    product = productRepository.save(product);

    // Assert
    assertFalse(reviewRepository.findById(review.getId()).isPresent());
    Product retrievedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertEquals(0, retrievedProduct.getReviewIds().size());
  }
}
