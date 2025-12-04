package com.nexus.controller;

import com.nexus.auth.service.UserAuthService;
import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.model.Company;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import com.nexus.repository.CompanyRepository;
import com.nexus.sentiment.SentimentService;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/products")
public final class ProductController {

  private static final Logger log = LoggerFactory.getLogger(ProductController.class);

  private final ProductRepository productRepository;
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final CompanyRepository companyRepository;
  private final SentimentService sentimentService;

  /**
   * Service for user authentication.
   */
  private final UserAuthService userAuthService;

  /**
   * Constructs a ProductController with the given repositories and services.
   *
   * @param productRepo      the repository for Product entities
   * @param reviewRepo       the repository for Review entities
   * @param userRepo         the repository for User entities
   * @param companyRepo      the repository for Company entities
   * @param newSentimentService the service for sentiment analysis
   * @param newUserAuthService  the service for user authentication
   */
  public ProductController(final ProductRepository productRepo,
                           final ReviewRepository reviewRepo,
                           final UserRepository userRepo,
                           final CompanyRepository companyRepo,
                           final SentimentService newSentimentService,
      final UserAuthService newUserAuthService) {
    this.productRepository = productRepo;
    this.reviewRepository = reviewRepo;
    this.userRepository = userRepo;
    this.companyRepository = companyRepo;
    this.sentimentService = newSentimentService;
    this.userAuthService = newUserAuthService;
  }

  /**
   * Creates a new product.
   *
   * @param userId  the authenticated user ID (required header)
   * @param product Product to create
   * @return ResponseEntity containing created product or error message
   */
  @PostMapping
  public ResponseEntity<?> createProduct(
      @RequestHeader("X-User-Id") final String userId,
      @RequestBody final Product product) {
    if (log.isInfoEnabled()) {
      log.info("Received request to create product: {}", product);
    }

    try {
      // Validate user exists
      userAuthService.validateUser(userId);
      if (product.getReviewIds() == null) {
        product.setReviewIds(new ArrayList<>());
      }

      if (log.isDebugEnabled()) {
        log.debug("Saving product to database");
      }
      Product saved = productRepository.save(product);

      // Attach product to company if applicable
      if (saved.getCompanyName() != null && !saved.getCompanyName().isBlank()) {
        companyRepository.findByName(saved.getCompanyName())
        .ifPresent(company -> {
          if (log.isInfoEnabled()) {
            log.info("Associating product {} with company {}", saved.getId(), company.getName());
          }

          if (company.getProducts() == null) {
            company.setProducts(new ArrayList<>());
          }
          if (!company.getProducts().contains(saved.getId())) {
            company.getProducts().add(saved.getId());
            companyRepository.save(company);
          }
        });
      }

      return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (IllegalStateException ise) {
      // User validation failed
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());
    } catch (IllegalArgumentException iae) {
      // Invalid user ID format
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid user ID: " + iae.getMessage());

    } catch (DataAccessException dae) {
      log.error("Database error while saving product", dae);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while saving product: " + dae.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error while creating product", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: " + e.getMessage());
    }
  }

  /**
   * Retrieves all products.
   *
   * @param userId the authenticated user ID (required header)
   * @return ResponseEntity containing the list of products
   */
  @GetMapping
  public ResponseEntity<?> getAllProducts(
      @RequestHeader("X-User-Id") final String userId) {
    if (log.isInfoEnabled()) {
      log.info("Received request to fetch all products");
    }

    try {
      List<Product> products = productRepository.findAll();

      if (log.isDebugEnabled()) {
        log.debug("Fetched {} products", products.size());
      }
      // Validate user exists
      userAuthService.validateUser(userId);
      return ResponseEntity.ok(products);
    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());
    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid user ID: " + iae.getMessage());

    } catch (Exception e) {
      log.error("Unexpected error while fetching products", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(List.of());
    }
  }

  /**
   * Posts a review for a product.
   *
   * @param userId    the authenticated user ID (required header)
   * @param productId Product ID
   * @param review    Review body
   * @return ResponseEntity with created review or error message
   */
  @PostMapping("/{productId}/reviews")
  public ResponseEntity<?> postReview(
      @RequestHeader("X-User-Id") final String userId,
      @PathVariable final String productId,
                                      @RequestBody final Review review) {

    if (log.isInfoEnabled()) {
      log.info("Posting review for productId={} review={}", productId, review);
    }

    try {
      // Validate user exists
      userAuthService.validateUser(userId);

      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

      // Save user if needed
      if (review.getUser() != null &&
          (review.getUser().getId() == null || review.getUser().getId().isBlank())) {

        if (log.isDebugEnabled()) {
          log.debug("Review contains new user: {}", review.getUser());
        }

        userRepository.findByUsername(review.getUser().getUsername())
            .ifPresentOrElse(
                review::setUser,
                () -> userRepository.save(review.getUser()));
      }

      // Sentiment rating calculation
      if (review.getRating() == 0 &&
          review.getComment() != null
      &&
          !review.getComment().isBlank()) {

        if (!sentimentService.isTrained()) {
          if (log.isInfoEnabled()) {
            log.info("Sentiment model not trained. Training...");
          }
          sentimentService.trainModel(null, null, null);
        }

        double score = sentimentService.scoreFromText(review.getComment());
        review.setRating(score);
      }

      Review saved = reviewRepository.save(review);

      // Update product review list
      if (product.getReviewIds() == null) {
        product.setReviewIds(new ArrayList<>());
      }
      product.getReviewIds().add(saved.getId());

      // Recalculate product rating
      List<Review> productReviews = reviewRepository
      .findByIdIn(product.getReviewIds());
      double productAvgRating = productReviews.stream()
          .mapToDouble(Review::getRating)
          .average()
          .orElse(0.0);

      product.setRating(productAvgRating);
      productRepository.save(product);

      // Update company rating
      if (product.getCompanyName() != null
      && !product.getCompanyName().isBlank()) {
        List<Company> companies = companyRepository
        .findByProductsContaining(productId);

        for (Company company : companies) {
          List<Product> companyProducts = productRepository
          .findAllById(company.getProducts());
          double companyAvgRating = companyProducts.stream()
              .mapToDouble(Product::getRating)
              .average()
              .orElse(0.0);

          company.setRating(companyAvgRating);
          companyRepository.save(company);

          if (log.isDebugEnabled()) {
            log.debug("Updated company {} average rating to {}", company.getName(), companyAvgRating);
          }
        }
      }

      return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (IllegalStateException ise) {
      // User validation failed or product not found
      if (ise.getMessage().contains("does not exist")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Authentication failed: " + ise.getMessage());
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ise.getMessage());

    } catch (IllegalArgumentException iae) {
      log.warn("Invalid request: {}", iae.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
    } catch (DataAccessException dae) {
      log.error("Database error while posting review", dae);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while posting review: " + dae.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error while posting review", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: " + e.getMessage());
    }
  }

  /**
   * Retrieves reviews for a product.
   *
   * @param userId    the authenticated user ID (required header)
   * @param productId Product ID
   * @return ResponseEntity containing list of reviews or error message
   */
  @GetMapping("/{productId}/reviews")
  public ResponseEntity<?> getReviews(
      @RequestHeader("X-User-Id") final String userId,
      @PathVariable final String productId) {

    if (log.isInfoEnabled()) {
      log.info("Fetching reviews for productId={}", productId);
    }

    try {
      // Validate user exists
      userAuthService.validateUser(userId);

      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

      if (product.getReviewIds() == null || product.getReviewIds().isEmpty()) {
        return ResponseEntity.ok(List.of());
      }

      List<Review> reviews = reviewRepository
      .findByIdIn(product.getReviewIds());
      return ResponseEntity.ok(reviews);

    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());
    } catch (IllegalArgumentException iae) {
      log.warn("Invalid request: {}", iae.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error while fetching reviews", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: " + e.getMessage());
    }
  }

  /**
   * Updates an existing review.
   *
   * @param userId    the authenticated user ID (required header)
   * @param productId Product ID
   * @param reviewId  Review ID
   * @param update    Updated review object
   * @return ResponseEntity containing updated review or error message
   */
  @PutMapping("/{productId}/reviews/{reviewId}")
  public ResponseEntity<?> updateReview(
      @RequestHeader("X-User-Id") final String userId,
      @PathVariable final String productId,
                                        @PathVariable final String reviewId,
                                        @RequestBody final Review update) {

    if (log.isInfoEnabled()) {
      log.info("Updating review {} for product {}", reviewId, productId);
    }

    try {
      // Validate user exists
      userAuthService.validateUser(userId);

      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

      Review existing = reviewRepository.findById(reviewId)
          .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

      existing.setComment(update.getComment());
      existing.setRating(update.getRating());
      if (update.getUser() != null) {
        existing.setUser(update.getUser());
      }

      Review saved = reviewRepository.save(existing);

      // Recalculate product rating
      List<Review> productReviews = reviewRepository
      .findByIdIn(product.getReviewIds());
      double productAvgRating = productReviews.stream()
          .mapToDouble(Review::getRating)
          .average()
          .orElse(0.0);

      product.setRating(productAvgRating);
      productRepository.save(product);

      if (product.getCompanyName() != null
      && !product.getCompanyName().isBlank()) {
        List<Company> companies = companyRepository
        .findByProductsContaining(productId);
        for (Company company : companies) {
          List<Product> companyProducts = productRepository
          .findAllById(company.getProducts());
          double companyAvgRating = companyProducts.stream()
              .mapToDouble(Product::getRating)
              .average()
              .orElse(0.0);
          company.setRating(companyAvgRating);
          companyRepository.save(company);
        }
      }

      return ResponseEntity.ok(saved);

    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());
    } catch (IllegalArgumentException iae) {
      log.warn("Invalid input while updating review: {}", iae.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(iae.getMessage());
    } catch (DataAccessException dae) {
      log.error("Database error while updating review", dae);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while updating review: " + dae.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error while updating review", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: " + e.getMessage());
    }
  }

  /**
   * Returns the average rating of a product.
   *
   * @param userId    the authenticated user ID (required header)
   * @param productId Product ID
   * @return ResponseEntity with average rating
   */
  @GetMapping("/{productId}/average-rating")
  public ResponseEntity<?> getAverageRating(
      @RequestHeader("X-User-Id") final String userId,
      @PathVariable final String productId) {

    if (log.isInfoEnabled()) {
      log.info("Fetching average rating for productId={}", productId);
    }

    try {
      // Validate user exists
      userAuthService.validateUser(userId);

      Product product = productRepository.findById(productId)
          .orElseThrow(() -> new RuntimeException("Product not found"));

      return ResponseEntity.ok(product.getRating());
    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());

    } catch (DataAccessException dae) {
      log.error("Database error while fetching product rating", dae);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while fetching product: " + dae.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error while fetching product rating", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred: " + e.getMessage());
    }
  }
}
