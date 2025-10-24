package com.nexus.controller;

import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

  private final CompanyRepository companyRepository;

  public CompanyController(CompanyRepository companyRepository) {
    this.companyRepository = companyRepository;
  }

  @PostMapping
  public ResponseEntity<?> createCompany(@RequestBody Company company) {
    try {
      if (company == null || company.getName() == null || company.getName().isBlank()) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body("Invalid company data. 'name' field is required.");
      }

      Company savedCompany = companyRepository.save(company);
      return ResponseEntity.status(HttpStatus.CREATED).body(savedCompany);

    } catch (DataAccessException dae) {
      // Handles database-related issues
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while saving company: " + dae.getMessage());

    } catch (Exception e) {
      // Catch-all for other unexpected exceptions
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred: " + e.getMessage());
    }
  }
}
