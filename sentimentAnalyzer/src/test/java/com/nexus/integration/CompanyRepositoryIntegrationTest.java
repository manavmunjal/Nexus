package com.nexus.integration;

import com.nexus.model.Company;
import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Company repository and model interactions.
 * Tests the data layer without mocking.
 */
@DataMongoTest
class CompanyRepositoryIntegrationTest {

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @BeforeEach
  void setUp() {
    // Clean up before each test
    reviewRepository.deleteAll();
    productRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    // Clean up after each test
    reviewRepository.deleteAll();
    productRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @Test
  void testSaveAndRetrieveCompany() {
    // Arrange
    Company company = new Company();
    company.setName("Test Company");
    company.setProducts(new ArrayList<>());

    // Act
    Company savedCompany = companyRepository.save(company);

    // Assert
    assertNotNull(savedCompany.getId());
    assertEquals("Test Company", savedCompany.getName());

    // Verify retrieval
    Company retrievedCompany = companyRepository.findById(savedCompany.getId()).orElse(null);
    assertNotNull(retrievedCompany);
    assertEquals("Test Company", retrievedCompany.getName());
  }

  @Test
  void testFindCompanyByName() {
    // Arrange
    Company company = new Company();
    company.setName("Unique Company Name");
    companyRepository.save(company);

    // Act
    Company foundCompany = companyRepository.findByName("Unique Company Name").orElse(null);

    // Assert
    assertNotNull(foundCompany);
    assertEquals("Unique Company Name", foundCompany.getName());
  }

  @Test
  void testCompanyWithMultipleProducts() {
    // Arrange
    Company company = new Company();
    company.setName("Multi Product Company");
    company.setProducts(new ArrayList<>());
    company = companyRepository.save(company);

    Product product1 = new Product();
    product1.setName("Product 1");
    product1 = productRepository.save(product1);

    Product product2 = new Product();
    product2.setName("Product 2");
    product2 = productRepository.save(product2);

    // Act
    company.getProducts().add(product1.getId());
    company.getProducts().add(product2.getId());
    company = companyRepository.save(company);

    // Assert
    Company retrievedCompany = companyRepository.findById(company.getId()).orElseThrow();
    assertEquals(2, retrievedCompany.getProducts().size());
    assertTrue(retrievedCompany.getProducts().contains(product1.getId()));
    assertTrue(retrievedCompany.getProducts().contains(product2.getId()));
  }

  @Test
  void testCompanyRatingCalculation() {
    // Arrange
    Company company = new Company();
    company.setName("Rated Company");
    company.setProducts(new ArrayList<>());
    company = companyRepository.save(company);

    // Create products with ratings
    Product product1 = new Product();
    product1.setName("Product 1");
    product1.setRating(4.5);
    product1 = productRepository.save(product1);

    Product product2 = new Product();
    product2.setName("Product 2");
    product2.setRating(3.5);
    product2 = productRepository.save(product2);

    company.getProducts().add(product1.getId());
    company.getProducts().add(product2.getId());

    // Act
    List<Product> products = productRepository.findAllById(company.getProducts());
    double avgRating = products.stream()
        .mapToDouble(Product::getRating)
        .average()
        .orElse(0.0);
    company.setRating(avgRating);
    company = companyRepository.save(company);

    // Assert
    Company retrievedCompany = companyRepository.findById(company.getId()).orElseThrow();
    assertEquals(4.0, retrievedCompany.getRating(), 0.01);
  }

  @Test
  void testFindCompaniesByProductId() {
    // Arrange
    Product product = new Product();
    product.setName("Shared Product");
    product = productRepository.save(product);

    Company company1 = new Company();
    company1.setName("Company 1");
    company1.setProducts(List.of(product.getId()));
    companyRepository.save(company1);

    Company company2 = new Company();
    company2.setName("Company 2");
    company2.setProducts(List.of(product.getId()));
    companyRepository.save(company2);

    // Act
    List<Company> companies = companyRepository.findByProductsContaining(product.getId());

    // Assert
    assertEquals(2, companies.size());
  }

  @Test
  void testDeleteCompany() {
    // Arrange
    Company company = new Company();
    company.setName("Company to Delete");
    company = companyRepository.save(company);
    String companyId = company.getId();

    // Act
    companyRepository.deleteById(companyId);

    // Assert
    assertFalse(companyRepository.findById(companyId).isPresent());
  }

  @Test
  void testCompanyWithProductsAndReviews() {
    // Arrange
    Company company = new Company();
    company.setName("Full Test Company");
    company.setProducts(new ArrayList<>());
    company = companyRepository.save(company);

    Product product = new Product();
    product.setName("Product with Reviews");
    product.setReviewIds(new ArrayList<>());
    product = productRepository.save(product);

    Review review1 = new Review();
    review1.setComment("Great!");
    review1.setRating(5);
    review1 = reviewRepository.save(review1);

    Review review2 = new Review();
    review2.setComment("Good");
    review2.setRating(4);
    review2 = reviewRepository.save(review2);

    // Act
    product.getReviewIds().add(review1.getId());
    product.getReviewIds().add(review2.getId());
    product = productRepository.save(product);

    company.getProducts().add(product.getId());
    company = companyRepository.save(company);

    // Assert
    Company retrievedCompany = companyRepository.findById(company.getId()).orElseThrow();
    assertEquals(1, retrievedCompany.getProducts().size());

    Product retrievedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertEquals(2, retrievedProduct.getReviewIds().size());

    List<Review> reviews = reviewRepository.findByIdIn(retrievedProduct.getReviewIds());
    assertEquals(2, reviews.size());
  }
}
