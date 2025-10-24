package com.nexus.controller;

import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Product entities and their associated reviews.
 * Provides endpoints for creating products, retrieving products,
 * posting reviews, and updating reviews.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
  private final ProductRepository productRepository;
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;

  /**
   * Constructs a ProductController with the specified repositories.
   *
   * @param productRepository the repository for product operations
   * @param reviewRepository the repository for review operations
   * @param userRepository the repository for user operations
   */
  public ProductController(ProductRepository productRepository, 
                         ReviewRepository reviewRepository, 
                         UserRepository userRepository) {
  this.productRepository = productRepository;
  this.reviewRepository = reviewRepository;
  this.userRepository = userRepository;
  }

  /**
   * Creates a new product.
   *
   * @param product the product to create
   * @return the created product, or null if an error occurs
   */
  @PostMapping
  public Product createProduct(@RequestBody Product product) {
  try {
  if (product.getReviewIds() == null) {
      product.setReviewIds(new ArrayList<>());
  }
  return productRepository.save(product);
  } catch (Exception e) {
  return null;
  }
  }

  /**
   * Retrieves all products.
   *
   * @return a list of all products, or an empty list if an error occurs
   */
  @GetMapping
  public List<Product> getAllProducts() {
  try {
  return productRepository.findAll();
  } catch (Exception e) {
  return List.of();
  }
  }

  /**
   * Posts a new review for a specific product.
   *
   * @param productId the ID of the product
   * @param review the review to post
   * @return the created review, or null if an error occurs
   */
  @PostMapping("/{productId}/reviews")
  public Review postReview(@PathVariable String productId, @RequestBody Review review) {
  try {
  Optional<Product> p = productRepository.findById(productId);
  if (p.isEmpty()) {
      throw new IllegalArgumentException("Product not found: " + productId);
  }

  // Save user first if present and missing id
  if (review.getUser() != null 
    && (review.getUser().getId() == null || review.getUser().getId().isBlank())) {
      userRepository.save(review.getUser());
  }

  Review saved = reviewRepository.save(review);
  Product product = p.get();
  if (product.getReviewIds() == null) {
      product.setReviewIds(new ArrayList<>());
  }
  product.getReviewIds().add(saved.getId());
  productRepository.save(product);
  return saved;
  } catch (Exception e) {
  return null;
  }   
  }

  /**
   * Retrieves all reviews for a specific product.
   *
   * @param productId the ID of the product
   * @return a list of reviews for the product, or an empty list if an error occurs
   */
  @GetMapping("/{productId}/reviews")
  public List<Review> getReviews(@PathVariable String productId) {
  try {
  Product product = productRepository.findById(productId)
    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
  if (product.getReviewIds() == null || product.getReviewIds().isEmpty()) {
      return List.of();
  }
  return reviewRepository.findByIdIn(product.getReviewIds());
  } catch (Exception e) {
  return List.of();
  }
  }

  /**
   * Updates an existing review for a specific product.
   *
   * @param productId the ID of the product
   * @param reviewId the ID of the review to update
   * @param update the review data to update
   * @return the updated review, or null if an error occurs
   */
  @PutMapping("/{productId}/reviews/{reviewId}")
  public Review updateReview(@PathVariable String productId, 
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
  return reviewRepository.save(existing);
  } catch (Exception e) {
  return null;
  }
  }
}
