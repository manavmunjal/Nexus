package com.nexus.controller;

import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductRepository productRepository;
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;

  public ProductController(ProductRepository productRepository,
                           ReviewRepository reviewRepository,
                           UserRepository userRepository) {
    this.productRepository = productRepository;
    this.reviewRepository = reviewRepository;
    this.userRepository = userRepository;
  }

  @PostMapping
  public ResponseEntity<?> createProduct(@RequestBody Product product) {
    try {
      if (product.getReviewIds() == null) {
        product.setReviewIds(new ArrayList<>());
      }
      Product saved = productRepository.save(product);
      return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (DataAccessException dae) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while saving product: " + dae.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: " + e.getMessage());
    }
  }

  @GetMapping
  public ResponseEntity<List<Product>> getAllProducts() {
    try {
      return ResponseEntity.ok(productRepository.findAll());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
    }
  }

  @PostMapping("/{productId}/reviews")
  public ResponseEntity<?> postReview(@PathVariable String productId,
                                      @RequestBody Review review) {
    try {
      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

      // Save user first if present and missing id
      if (review.getUser() != null &&
          (review.getUser().getId() == null || review.getUser().getId().isBlank())) {
        userRepository.save(review.getUser());
      }

      Review saved = reviewRepository.save(review);

      if (product.getReviewIds() == null) {
        product.setReviewIds(new ArrayList<>());
      }
      product.getReviewIds().add(saved.getId());
      productRepository.save(product);

      return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
    } catch (DataAccessException dae) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while posting review: " + dae.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: " + e.getMessage());
    }
  }

  @GetMapping("/{productId}/reviews")
  public ResponseEntity<?> getReviews(@PathVariable String productId) {
    try {
      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

      if (product.getReviewIds() == null || product.getReviewIds().isEmpty()) {
        return ResponseEntity.ok(List.of());
      }

      List<Review> reviews = reviewRepository.findByIdIn(product.getReviewIds());
      return ResponseEntity.ok(reviews);

    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: " + e.getMessage());
    }
  }

  @PutMapping("/{productId}/reviews/{reviewId}")
  public ResponseEntity<?> updateReview(@PathVariable String productId,
                                        @PathVariable String reviewId,
                                        @RequestBody Review update) {
    try {
      productRepository.findById(productId)
          .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

      Review existing = reviewRepository.findById(reviewId)
          .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

      existing.setComment(update.getComment());
      existing.setRating(update.getRating());
      if (update.getUser() != null) {
        existing.setUser(update.getUser());
      }

      Review saved = reviewRepository.save(existing);
      return ResponseEntity.ok(saved);

    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
    } catch (DataAccessException dae) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while updating review: " + dae.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: " + e.getMessage());
    }
  }
}
