package com.nexus.controller;

import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/companies")
public final class CompanyController {

  /**
   * Repository for Company entities.
   */
  private final CompanyRepository companyRepository;

  /**
   * Constructs a CompanyController with the given repository.
   *
   * @param repository the repository for Company entities
   */
  public CompanyController(final CompanyRepository repository) {
    this.companyRepository = repository;
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
}
