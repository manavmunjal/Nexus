package com.nexus.controller;

import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ProductController(ProductRepository productRepository, ReviewRepository reviewRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        try {
            if (product.getReviewIds() == null) product.setReviewIds(new ArrayList<>());
            return productRepository.save(product);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping
    public List<Product> getAllProducts() {
        try {
            return productRepository.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    @PostMapping("/{productId}/reviews")
    public Review postReview(@PathVariable String productId, @RequestBody Review review) {
        try {
            Optional<Product> p = productRepository.findById(productId);
            if (p.isEmpty()) throw new IllegalArgumentException("Product not found: " + productId);

            // Save user first if present and missing id
            if (review.getUser() != null && (review.getUser().getId() == null || review.getUser().getId().isBlank())) {
                userRepository.save(review.getUser());
            }

            Review saved = reviewRepository.save(review);
            Product product = p.get();
            if (product.getReviewIds() == null) product.setReviewIds(new ArrayList<>());
            product.getReviewIds().add(saved.getId());
            productRepository.save(product);
            return saved;
        } catch (Exception e) {
            return null;
        }   
    }

    @GetMapping("/{productId}/reviews")
    public List<Review> getReviews(@PathVariable String productId) {
        try {
            Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
            if (product.getReviewIds() == null || product.getReviewIds().isEmpty()) return List.of();
            return reviewRepository.findByIdIn(product.getReviewIds());
        } catch (Exception e) {
            return List.of();
        }
    }

    @PutMapping("/{productId}/reviews/{reviewId}")
    public Review updateReview(@PathVariable String productId, @PathVariable String reviewId, @RequestBody Review update) {
        // ensure product exists
        try {
            productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
            Review existing = reviewRepository.findById(reviewId).orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
            existing.setComment(update.getComment());
            existing.setRating(update.getRating());
            if (update.getUser() != null) existing.setUser(update.getUser());
            return reviewRepository.save(existing);
        } catch (Exception e) {
            return null;
        }
    }
}
