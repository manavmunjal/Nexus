package com.nexus.controller;

import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CompanyControllerTest {

  @Mock
  private CompanyRepository companyRepository;

  @InjectMocks
  private CompanyController companyController;

  private Company testCompany;

  @BeforeEach
  void setUp() {
      testCompany = new Company();
      testCompany.setId("1");
      testCompany.setName("Test Company");
  }

  @Test
  void createCompany_ShouldSaveAndReturnCompany() {
      when(companyRepository.save(any(Company.class))).thenReturn(testCompany);

      Company result = companyController.createCompany(testCompany);

      assertNotNull(result);
      assertEquals("1", result.getId());
      assertEquals("Test Company", result.getName());
      verify(companyRepository).save(testCompany);
  }
}