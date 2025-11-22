package com.nexus.controller;

import com.nexus.model.Company;
import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/companies")
public final class CompanyController {

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
   * Constructs a CompanyController with the given repositories.
   *
   * @param companyRepo the repository for Company entities
   * @param productRepo the repository for Product entities
   * @param reviewRepo  the repository for Review entities
   */
  public CompanyController(final CompanyRepository companyRepo,
      final ProductRepository productRepo,
      final ReviewRepository reviewRepo) {
    this.companyRepository = companyRepo;
    this.productRepository = productRepo;
    this.reviewRepository = reviewRepo;
  }

  /**
   * Creates a new company entity. This method is not designed for extension;
   * overriding may break request handling logic.
   *
   * @param company the company object to be created
   * @return ResponseEntity with status and body depending on the result
   */
  @PostMapping
  public ResponseEntity<?> createCompany(@RequestBody final Company company) {
    try {
      if (company == null || company.getName() == null
          || company.getName().isBlank()) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body("Invalid company data. 'name' field is required.");
      }

      Company savedCompany = companyRepository.save(company);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(savedCompany);

    } catch (DataAccessException dae) {
      // Handles database-related issues
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while saving company: "
              + dae.getMessage());

    } catch (Exception e) {
      // Catch-all for other unexpected exceptions
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred: "
              + e.getMessage());
    }
  }

  /**
   * Returns the average rating of a company (auto-updated when reviews are added
   * to products).
   *
   * @param companyId the unique identifier for the company
   * @return ResponseEntity with status and body depending on the result
   */
  @GetMapping("/{companyId}/average-rating")
  public ResponseEntity<?> getAverageRating(@PathVariable final String companyId) {
    try {
      Company company = companyRepository.findById(companyId)
          .orElseThrow(() -> new RuntimeException("Company not found"));
      return ResponseEntity.ok(company.getRating());
    } catch (DataAccessException dae) {
      // Handles database-related issues
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while fetching company: "
              + dae.getMessage());
    } catch (Exception e) {
      // Catch-all for other unexpected exceptions
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred: "
              + e.getMessage());
    }
  }

  /**
   * Return all reviews of a company.
   *
   * @param companyId the unique identifier for the company
   * @return ResponseEntity with status and body depending on the result
   */
  @GetMapping("/{companyId}/reviews")
  public ResponseEntity<?> getAllReviews(@PathVariable final String companyId) {
    try {
      Company company = companyRepository.findById(companyId)
          .orElseThrow(() -> new RuntimeException("Company not found"));

      List<String> productIds = company.getProducts();
      if (productIds == null || productIds.isEmpty()) {
        return ResponseEntity.ok(List.of());
      }

      List<Product> products = productRepository.findAllById(productIds);
      List<String> reviewIds = products.stream()
          .map(Product::getReviewIds)
          .filter(Objects::nonNull)
          .flatMap(List::stream)
          .collect(Collectors.toList());

      List<Review> reviews = reviewRepository.findAllById(reviewIds);
      return ResponseEntity.ok(reviews);
    } catch (DataAccessException dae) {
      // Handles database-related issues
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while fetching company: "
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
