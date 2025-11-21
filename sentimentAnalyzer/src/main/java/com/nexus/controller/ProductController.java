package com.nexus.controller;

import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.model.Company;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import com.nexus.repository.CompanyRepository;
import com.nexus.sentiment.SentimentService;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/products")
public final class ProductController {

  /**
   * Repository for Product entities.
   */
  private final ProductRepository productRepository;
  /**
   * Repository for Review entities.
   */
  private final ReviewRepository reviewRepository;
  /**
   * Repository for User entities.
   */
  private final UserRepository userRepository;
  /**
   * Repository for Company entities.
   */
  private final CompanyRepository companyRepository;
  /**
   * Service for sentiment analysis.
   */
  private final SentimentService sentimentService;

  /**
   * Constructs a ProductController with the given repositories.
   *
   * @param productRepo      the repository for Product entities
   * @param reviewRepo       the repository for Review entities
   * @param userRepo         the repository for User entities
   * @param companyRepo      the repository for Company entities
   * @param sentimentService the service for sentiment analysis
   */
  public ProductController(final ProductRepository productRepo,
      final ReviewRepository reviewRepo,
      final UserRepository userRepo,
      final CompanyRepository companyRepo,
      final SentimentService sentimentService) {
    this.productRepository = productRepo;
    this.reviewRepository = reviewRepo;
    this.userRepository = userRepo;
    this.companyRepository = companyRepo;
    this.sentimentService = sentimentService;
  }

  /**
   * Creates a new product entity. Not designed for extension.
   *
   * @param product the product object to be created
   * @return ResponseEntity with status and body depending on the result
   */
  @PostMapping
  public ResponseEntity<?> createProduct(@RequestBody final Product product) {
    try {
      if (product.getReviewIds() == null) {
        product.setReviewIds(new ArrayList<>());
      }
      Product saved = productRepository.save(product);

      // Add product to company's products array if companyName is specified
      if (saved.getCompanyName() != null && !saved.getCompanyName().isBlank()) {
        companyRepository.findByName(saved.getCompanyName()).ifPresent(company -> {
          if (company.getProducts() == null) {
            company.setProducts(new ArrayList<>());
          }
          if (!company.getProducts().contains(saved.getId())) {
            company.getProducts().add(saved.getId());
            companyRepository.save(company);
          }
        });
      }

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(saved);
    } catch (DataAccessException dae) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while saving product: "
              + dae.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: "
              + e.getMessage());
    }
  }

  /**
   * Retrieves all products. Not designed for extension.
   *
   * @return ResponseEntity with list of products or empty list on error
   */
  @GetMapping
  public ResponseEntity<List<Product>> getAllProducts() {
    try {
      return ResponseEntity.ok(productRepository.findAll());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(List.of());
    }
  }

  /**
   * Posts a review for a product. Not designed for extension.
   *
   * @param productId the product ID
   * @param review    the review object
   * @return ResponseEntity with status and body depending on the result
   */
  @PostMapping("/{productId}/reviews")
  public ResponseEntity<?> postReview(@PathVariable final String productId,
      @RequestBody final Review review) {
    try {
      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new IllegalArgumentException(
              "Product not found: " + productId));

      // Save user first if present and missing id
      if (review.getUser() != null
          && (review.getUser().getId() == null
              || review.getUser().getId().isBlank())) {
        userRepository.findByUsername(review.getUser().getUsername())
            .ifPresentOrElse(
                review::setUser,
                () -> userRepository.save(review.getUser()));
      }

      // Calculate rating from comment if not provided
      if (review.getRating() == 0 && review.getComment() != null && !review.getComment().isBlank()) {
        if (!sentimentService.isTrained()) {
          sentimentService.trainModel(null, null, null);
        }
        double score = sentimentService.scoreFromText(review.getComment());
        review.setRating(score);
      } else if (review.getRating() == 0) {
        review.setRating(0);
      }

      Review saved = reviewRepository.save(review);

      if (product.getReviewIds() == null) {
        product.setReviewIds(new ArrayList<>());
      }
      product.getReviewIds().add(saved.getId());

      // Update product average rating
      List<Review> productReviews = reviewRepository.findByIdIn(product.getReviewIds());
      double productAvgRating = productReviews.stream()
          .mapToDouble(Review::getRating)
          .average()
          .orElse(0.0);
      product.setRating(productAvgRating);
      productRepository.save(product);

      // Update company average rating if product belongs to a company
      if (product.getCompanyName() != null && !product.getCompanyName().isBlank()) {
        List<Company> companies = companyRepository.findByProductsContaining(productId);
        for (Company company : companies) {
          List<Product> companyProducts = productRepository.findAllById(company.getProducts());
          double companyAvgRating = companyProducts.stream()
              .mapToDouble(Product::getRating)
              .average()
              .orElse(0.0);
          company.setRating(companyAvgRating);
          companyRepository.save(company);
        }
      }

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(saved);
    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
    } catch (DataAccessException dae) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while posting review: "
              + dae.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: "
              + e.getMessage());
    }
  }

  /**
   * Retrieves reviews for a product. Not designed for extension.
   *
   * @param productId the product ID
   * @return ResponseEntity with list of reviews or error message
   */
  @GetMapping("/{productId}/reviews")
  public ResponseEntity<?> getReviews(@PathVariable final String productId) {
    try {
      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new IllegalArgumentException(
              "Product not found: " + productId));

      if (product.getReviewIds() == null || product.getReviewIds().isEmpty()) {
        return ResponseEntity.ok(List.of());
      }

      List<Review> reviews = reviewRepository.findByIdIn(product.getReviewIds());
      return ResponseEntity.ok(reviews);

    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: "
              + e.getMessage());
    }
  }

  /**
   * Updates a review for a product. Not designed for extension.
   *
   * @param productId the product ID
   * @param reviewId  the review ID
   * @param update    the review update object
   * @return ResponseEntity with updated review or error message
   */
  @PutMapping("/{productId}/reviews/{reviewId}")
  public ResponseEntity<?> updateReview(@PathVariable final String productId,
      @PathVariable final String reviewId,
      @RequestBody final Review update) {
    try {
      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new IllegalArgumentException(
              "Product not found: " + productId));

      Review existing = reviewRepository.findById(reviewId)
          .orElseThrow(() -> new IllegalArgumentException(
              "Review not found: " + reviewId));

      existing.setComment(update.getComment());
      existing.setRating(update.getRating());
      if (update.getUser() != null) {
        existing.setUser(update.getUser());
      }

      Review saved = reviewRepository.save(existing);

      // Recalculate product average rating
      List<Review> productReviews = reviewRepository.findByIdIn(product.getReviewIds());
      double productAvgRating = productReviews.stream()
          .mapToDouble(Review::getRating)
          .average()
          .orElse(0.0);
      product.setRating(productAvgRating);
      productRepository.save(product);

      // Recalculate company average rating if product belongs to a company
      if (product.getCompanyName() != null && !product.getCompanyName().isBlank()) {
        List<Company> companies = companyRepository.findByProductsContaining(productId);
        for (Company company : companies) {
          List<Product> companyProducts = productRepository.findAllById(company.getProducts());
          double companyAvgRating = companyProducts.stream()
              .mapToDouble(Product::getRating)
              .average()
              .orElse(0.0);
          company.setRating(companyAvgRating);
          companyRepository.save(company);
        }
      }

      return ResponseEntity.ok(saved);

    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
    } catch (DataAccessException dae) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while updating review: "
              + dae.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: "
              + e.getMessage());
    }
  }

  /**
   * Returns the average rating of a product (auto-updated when reviews are
   * added).
   *
   * @param productId the unique identifier for the product
   * @return ResponseEntity with status and body depending on the result
   */
  @GetMapping("/{productId}/average-rating")
  public ResponseEntity<?> getAverageRating(@PathVariable final String productId) {
    try {
      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new RuntimeException("Product not found"));
      return ResponseEntity.ok(product.getRating());
    } catch (DataAccessException dae) {
      // Handles database-related issues
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while fetching product: "
              + dae.getMessage());
    } catch (Exception e) {
      // Catch-all for other unexpected exceptions
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred: "
              + e.getMessage());
    }
  }
}
