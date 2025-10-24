package com.nexus.controller;

import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyControllerTest {

  private CompanyRepository companyRepository;
  private CompanyController companyController;

  @BeforeEach
  void setUp() {
    companyRepository = mock(CompanyRepository.class);
    companyController = new CompanyController(companyRepository);
  }

  @Test
  void createCompany_ShouldReturnCreated_WhenValidCompany() {
    // Arrange
    Company company = new Company();
    company.setName("OpenAI");

    when(companyRepository.save(company)).thenReturn(company);

    // Act
    ResponseEntity<?> response = companyController.createCompany(company);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(company, response.getBody());
    verify(companyRepository, times(1)).save(company);
  }

  @Test
  void createCompany_ShouldReturnBadRequest_WhenCompanyIsNull() {
    // Act
    ResponseEntity<?> response = companyController.createCompany(null);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Invalid company data"));
    verify(companyRepository, never()).save(any());
  }

  @Test
  void createCompany_ShouldReturnBadRequest_WhenCompanyNameIsEmpty() {
    // Arrange
    Company company = new Company();
    company.setName("  "); // blank name

    // Act
    ResponseEntity<?> response = companyController.createCompany(company);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Invalid company data"));
    verify(companyRepository, never()).save(any());
  }

  @Test
  void createCompany_ShouldReturnInternalServerError_WhenDatabaseErrorOccurs() {
    // Arrange
    Company company = new Company();
    company.setName("ErrorCorp");

    when(companyRepository.save(company))
        .thenThrow(new DataAccessException("DB down") {});

    // Act
    ResponseEntity<?> response = companyController.createCompany(company);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void createCompany_ShouldReturnInternalServerError_WhenUnexpectedExceptionOccurs() {
    // Arrange
    Company company = new Company();
    company.setName("FailCorp");

    when(companyRepository.save(company))
        .thenThrow(new RuntimeException("Unexpected failure"));

    // Act
    ResponseEntity<?> response = companyController.createCompany(company);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Unexpected error"));
  }
}
