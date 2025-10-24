package com.nexus.controller;

import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductControllerTest {

  private ProductRepository productRepository;
  private ReviewRepository reviewRepository;
  private UserRepository userRepository;
  private ProductController controller;

  @BeforeEach
  void setUp() {
    productRepository = mock(ProductRepository.class);
    reviewRepository = mock(ReviewRepository.class);
    userRepository = mock(UserRepository.class);
    controller = new ProductController(productRepository, reviewRepository, userRepository);
  }

  @Test
  void createProduct_ShouldReturnCreatedProduct() {
    Product product = new Product();
    product.setId("p1");

    when(productRepository.save(product)).thenReturn(product);

    ResponseEntity<?> response = controller.createProduct(product);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(product, response.getBody());
    verify(productRepository, times(1)).save(product);
  }

  @Test
  void createProduct_ShouldReturnInternalServerError_OnDatabaseException() {
    Product product = new Product();

    when(productRepository.save(product)).thenThrow(new DataAccessException("DB down") {});

    ResponseEntity<?> response = controller.createProduct(product);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  assertNotNull(response.getBody());
  assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void getAllProducts_ShouldReturnProductList() {
    List<Product> products = List.of(new Product(), new Product());
    when(productRepository.findAll()).thenReturn(products);

    ResponseEntity<List<Product>> response = controller.getAllProducts();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(products, response.getBody());
  }

  @Test
  void getReviews_ShouldReturnListOfReviews() {
    Product product = new Product();
    product.setId("p1");
    product.setReviewIds(List.of("r1"));

    Review review = new Review();
    review.setId("r1");

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findByIdIn(List.of("r1"))).thenReturn(List.of(review));

    ResponseEntity<?> response = controller.getReviews("p1");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(List.of(review), response.getBody());
  }

  @Test
  void getReviews_ShouldReturnNotFound_WhenProductMissing() {
    when(productRepository.findById("p1")).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.getReviews("p1");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  assertNotNull(response.getBody());
  assertTrue(response.getBody().toString().contains("Product not found"));
  }

  @Test
  void postReview_ShouldReturnNotFound_WhenProductMissing() {
    Review review = new Review();
    when(productRepository.findById("p1")).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.postReview("p1", review);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  assertNotNull(response.getBody());
  assertTrue(response.getBody().toString().contains("Product not found"));
  }

  @Test
  void updateReview_ShouldReturnUpdatedReview() {
    Product product = new Product();
    product.setId("p1");

    Review existing = new Review();
    existing.setId("r1");
    existing.setComment("Old");
    existing.setRating(3);

    Review update = new Review();
    update.setComment("New");
    update.setRating(5);

    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findById("r1")).thenReturn(Optional.of(existing));
    when(reviewRepository.save(existing)).thenReturn(existing);

    ResponseEntity<?> response = controller.updateReview("p1", "r1", update);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  Review saved = (Review) response.getBody();
  assertNotNull(saved);
  assertEquals("New", saved.getComment());
  assertEquals(5, saved.getRating());
  }

  @Test
  void updateReview_ShouldReturnNotFound_WhenReviewMissing() {
    Product product = new Product();
    product.setId("p1");

    Review update = new Review();
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(reviewRepository.findById("r1")).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.updateReview("p1", "r1", update);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  assertNotNull(response.getBody());
  assertTrue(response.getBody().toString().contains("Review not found"));
  }
}
