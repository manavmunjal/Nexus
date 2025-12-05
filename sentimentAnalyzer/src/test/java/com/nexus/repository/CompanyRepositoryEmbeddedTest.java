package com.nexus.repository;

import com.nexus.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class CompanyRepositoryEmbeddedTest {

  @Autowired
  private CompanyRepository companyRepository;

  @BeforeEach
  void setUp() {
    companyRepository.deleteAll();
  }

  @Test
  public void testSaveAndFindByName() {
    Company company = new Company();
    company.setName("Test Company");
    company.setProducts(Arrays.asList("prod1", "prod2"));

    Company savedCompany = companyRepository.save(company);

    assertThat(savedCompany.getId()).isNotNull();

    Optional<Company> foundCompany = companyRepository.findByName("Test Company");

    assertThat(foundCompany).isPresent();
    assertThat(foundCompany.get().getName()).isEqualTo("Test Company");
    assertThat(foundCompany.get().getProducts()).contains("prod1", "prod2");
  }

  @Test
  public void testFindByProductsContaining() {
    Company company1 = new Company();
    company1.setName("Company 1");
    company1.setProducts(Arrays.asList("prodA", "prodB"));

    Company company2 = new Company();
    company2.setName("Company 2");
    company2.setProducts(Arrays.asList("prodB", "prodC"));

    companyRepository.saveAll(Arrays.asList(company1, company2));

    List<Company> companiesWithProdB = companyRepository.findByProductsContaining("prodB");
    assertThat(companiesWithProdB).hasSize(2);

    List<Company> companiesWithProdA = companyRepository.findByProductsContaining("prodA");
    assertThat(companiesWithProdA).hasSize(1);
    assertThat(companiesWithProdA.get(0).getName()).isEqualTo("Company 1");
  }
}
