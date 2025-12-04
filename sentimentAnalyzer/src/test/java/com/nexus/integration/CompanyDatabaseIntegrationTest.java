package com.nexus.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.controller.CompanyController;
import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests verifying Database Interactions via Mocks for CompanyController.
 */
@ExtendWith(MockitoExtension.class)
public class CompanyDatabaseIntegrationTest {

  @Mock
  private CompanyRepository companyRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @InjectMocks
  private CompanyController companyController;

  @Test
  public void testCreateCompany_CallsDatabaseSave() {
    Company company = new Company();
    company.setName("Mock DB Test Company");

    when(companyRepository.save(any(Company.class))).thenReturn(company);

    companyController.createCompany(company);

    verify(companyRepository, times(1)).save(company);
  }

  @Test
  public void testDatabaseFailure_Returns500Error() {
    Company company = new Company();
    company.setName("Mock DB Test Company");

    when(companyRepository.save(any(Company.class)))
        .thenThrow(new DataAccessException("Connection refused") {
        });

    ResponseEntity<?> response = companyController.createCompany(company);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    verify(companyRepository, times(1)).save(company);
  }

  @Test
  public void testGetCompany_QueriesDatabaseById() {
    String companyId = "comp123";
    Company mockCompany = new Company();
    mockCompany.setId(companyId);
    mockCompany.setRating(4.0);

    when(companyRepository.findById(companyId)).thenReturn(Optional.of(mockCompany));

    companyController.getAverageRating(companyId);

    verify(companyRepository).findById(companyId);
  }
}
