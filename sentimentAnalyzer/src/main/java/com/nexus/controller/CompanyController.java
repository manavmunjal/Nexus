package com.nexus.controller;

import com.nexus.auth.service.UserAuthService;
import com.nexus.model.Company;
import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/companies")
public final class CompanyController {

  /**
   * Logger instance for CompanyController.
   */
  private static final Logger LOGGER =
  LoggerFactory.getLogger(CompanyController.class);

  /**
   * Repository for Company entities.
   */
  private final CompanyRepository companyRepository;

  /**
   * Repository for Product entities.
   */
  private final ProductRepository productRepository;

  /**
   * Repository for Review entities.
   */
  private final ReviewRepository reviewRepository;

  /**
   * Service for user authentication.
   */
  private final UserAuthService userAuthService;

  /**
   * Constructs a CompanyController with the given repositories and services.
   *
   * @param companyRepo     the repository for Company entities
   * @param productRepo     the repository for Product entities
   * @param reviewRepo      the repository for Review entities
   * @param newUserAuthService the service for user authentication
   */
  public CompanyController(final CompanyRepository companyRepo,
                           final ProductRepository productRepo,
                           final ReviewRepository reviewRepo,
      final UserAuthService newUserAuthService) {
    this.companyRepository = companyRepo;
    this.productRepository = productRepo;
    this.reviewRepository = reviewRepo;
    this.userAuthService = newUserAuthService;
  }

  /**
   * Creates a new company entity. This method is not designed for extension;
   * overriding may break request handling logic.
   *
   * @param userId  the authenticated user ID (required header)
   * @param company the company object to be created
   * @return ResponseEntity with status and body depending on the result
   */
  @PostMapping
  public ResponseEntity<?> createCompany(
      @RequestHeader("X-User-Id") final String userId,
      @RequestBody final Company company) {

    if (LOGGER.isInfoEnabled()) {
      // Validate user exists
      userAuthService.validateUser(userId);

      LOGGER.info("Received request to create company: {}",
          company != null ? company.getName() : "null");
    }

    try {
      if (company == null || company.getName() == null
      || company.getName().isBlank()) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn("Invalid company data received: {}", company);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Invalid company data. 'name' field is required.");
      }

      Company savedCompany = companyRepository.save(company);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
          "Successfully created company with ID={}",
          savedCompany.getId());
      }

      return ResponseEntity.status(HttpStatus.CREATED).body(savedCompany);

    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());

    } catch (IllegalArgumentException iae) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid user ID: " + iae.getMessage());

    } catch (DataAccessException dae) {
      LOGGER.error("Database error while creating company", dae);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while saving company: " + dae.getMessage());

    } catch (Exception e) {
      LOGGER.error("Unexpected error while creating company", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred: " + e.getMessage());
    }
  }

  /**
   * Returns the average rating of a company (auto-updated
   * when reviews are added to products).
   *
   * @param userId    the authenticated user ID (required header)
   * @param companyId the unique identifier for the company
   * @return ResponseEntity with status and body depending on the result
   */
  @GetMapping("/{companyId}/average-rating")
  public ResponseEntity<?> getAverageRating(
      @RequestHeader("X-User-Id") final String userId,
      @PathVariable final String companyId) {

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
        "Received request to fetch average rating for companyId={}",
        companyId);
    }

    try {
      // Validate user exists
      userAuthService.validateUser(userId);

      Company company = companyRepository.findById(companyId)
          .orElseThrow(() -> new RuntimeException("Company not found"));

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
          "Successfully fetched average rating for companyId={} rating={}",
            companyId, company.getRating());
      }

      return ResponseEntity.ok(company.getRating());
    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());

    } catch (DataAccessException dae) {
      LOGGER.error(
        "Database error while fetching company rating for companyId={}",
        companyId, dae);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while fetching company: " + dae.getMessage());

    } catch (Exception e) {
      LOGGER.error(
        "Unexpected error fetching company rating for companyId={}",
        companyId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred: " + e.getMessage());
    }
  }

  /**
   * Return all reviews of a company.
   *
   * @param userId    the authenticated user ID (required header)
   * @param companyId the unique identifier for the company
   * @return ResponseEntity with status and body depending on the result
   */
  @GetMapping("/{companyId}/reviews")
  public ResponseEntity<?> getAllReviews(
      @RequestHeader("X-User-Id") final String userId,
      @PathVariable final String companyId) {

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
        "Received request to fetch all reviews for companyId={}",
        companyId);
    }

    try {
      // Validate user exists
      userAuthService.validateUser(userId);

      Company company = companyRepository.findById(companyId)
          .orElseThrow(() -> new RuntimeException("Company not found"));

      List<String> productIds = company.getProducts();

      if (productIds == null || productIds.isEmpty()) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(
            "Company {} has no products -> returning empty review list",
            companyId);
        }
        return ResponseEntity.ok(List.of());
      }

      List<Product> products = productRepository.findAllById(productIds);

      List<String> reviewIds = products.stream()
          .map(Product::getReviewIds)
          .filter(Objects::nonNull)
          .flatMap(List::stream)
          .collect(Collectors.toList());

      List<Review> reviews = reviewRepository.findAllById(reviewIds);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Fetched {} reviews for companyId={}",
        reviews.size(), companyId);
      }

      return ResponseEntity.ok(reviews);
    } catch (IllegalStateException ise) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed: " + ise.getMessage());

    } catch (DataAccessException dae) {
      LOGGER.error(
        "Database error while fetching company reviews for companyId={}",
        companyId, dae);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while fetching company: " + dae.getMessage());

    } catch (Exception e) {
      LOGGER.error("Unexpected error fetching reviews for companyId={}",
      companyId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred: " + e.getMessage());
    }
  }
}
