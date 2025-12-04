package com.nexus.integration;

import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for CompanyRepository using embedded/in-memory MongoDB (Flapdoodle).
 *
 * <p>Tests saving, retrieving, and querying Company documents directly using the repository.
 */
@DataMongoTest
class CompanyRepositoryDatabaseIntegrationTest {

  @Autowired
  private CompanyRepository companyRepository;

  @AfterEach
  void tearDown() {
    companyRepository.deleteAll();
  }

  /**
   * Tests saving a company and retrieving it by ID.
   */
  @Test
  void shouldSaveAndRetrieveCompany() {
    Company company = new Company();
    company.setName("Test Company");
    company.setProducts(Arrays.asList("prod-1", "prod-2"));

    Company savedCompany = companyRepository.save(company);

    assertThat(savedCompany).isNotNull();
    assertThat(savedCompany.getId()).isNotNull();
    assertThat(savedCompany.getName()).isEqualTo("Test Company");
    assertThat(savedCompany.getProducts()).containsExactlyInAnyOrder("prod-1", "prod-2");

    Optional<Company> fetched = companyRepository.findById(savedCompany.getId());
    assertThat(fetched).isPresent();
    assertThat(fetched.get().getName()).isEqualTo("Test Company");
  }

  /**
   * Tests finding companies that contain a specific product ID.
   */
  @Test
  void shouldFindCompaniesByProductId() {
    Company c1 = new Company();
    c1.setName("Company A");
    c1.setProducts(Arrays.asList("prod-1", "prod-2"));

    Company c2 = new Company();
    c2.setName("Company B");
    c2.setProducts(Arrays.asList("prod-3"));

    Company c3 = new Company();
    c3.setName("Company C");
    c3.setProducts(Arrays.asList("prod-1", "prod-4"));

    companyRepository.saveAll(Arrays.asList(c1, c2, c3));

    List<Company> companiesWithProd1 = companyRepository.findByProductsContaining("prod-1");

    assertThat(companiesWithProd1).hasSize(2);
    assertThat(companiesWithProd1).extracting("name")
        .containsExactlyInAnyOrder("Company A", "Company C");
  }

  /**
   * Tests finding a company by its name.
   */
  @Test
  void shouldFindCompanyByName() {
    Company company = new Company();
    company.setName("Unique Company");
    company.setProducts(Arrays.asList("prod-5"));

    companyRepository.save(company);

    Optional<Company> found = companyRepository.findByName("Unique Company");

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Unique Company");
  }

  /**
   * Tests that searching for a non-existent name returns empty.
   */
  @Test
  void shouldReturnEmptyWhenNameNotFound() {
    Optional<Company> found = companyRepository.findByName("Nonexistent Company");
    assertThat(found).isEmpty();
  }

  /**
   * Tests that searching for a non-existent product ID returns empty list.
   */
  @Test
  void shouldReturnEmptyListWhenProductIdNotFound() {
    List<Company> companies = companyRepository.findByProductsContaining("non-existent-prod");
    assertThat(companies).isEmpty();
  }
}
