package com.nexus.controller;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;

@ExtendWith(OutputCaptureExtension.class)
class CompanyControllerLoggingTest {

  private CompanyRepository companyRepository;
  private ProductRepository productRepository;
  private ReviewRepository reviewRepository;
  private UserAuthService userAuthService;
  private CompanyController controller;
  private Company company;

  private static final String VALID_USER_ID = "test-user-123";

  @BeforeEach
  void setUp() {
    companyRepository = mock(CompanyRepository.class);
    productRepository = mock(ProductRepository.class);
    reviewRepository = mock(ReviewRepository.class);
    userAuthService = mock(UserAuthService.class);

    controller = new CompanyController(companyRepository, productRepository, reviewRepository, userAuthService);

    company = new Company();
    company.setId("c1");
    company.setName("TestCompany");
    company.setProducts(new ArrayList<>());

    when(userAuthService.validateUser(VALID_USER_ID)).thenReturn(new AuthUser(VALID_USER_ID));
  }

  @Test
  void createCompany_ShouldLogInfo_WhenRequestReceived(CapturedOutput output) {
    when(companyRepository.save(company)).thenReturn(company);

    controller.createCompany(VALID_USER_ID, company);

    assertTrue(output.getOut().contains("Received request to create company"), "Should log info message");
  }

  @Test
  void createCompany_ShouldLogError_WhenDatabaseException(CapturedOutput output) {
    when(companyRepository.save(company)).thenThrow(new DataAccessException("DB down") {
    });

    controller.createCompany(VALID_USER_ID, company);

    assertTrue(output.getOut().contains("Database error while creating company"), "Should log error message");
  }
}
