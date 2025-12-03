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
 * Integration tests for Product repository and model interactions.
 * Tests the data layer without mocking.
 */
@DataMongoTest
class ProductRepositoryIntegrationTest {

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private CompanyRepository companyRepository;

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
  void testSaveAndRetrieveProduct() {
    // Arrange
    Product product = new Product();
    product.setName("Test Product");
    product.setDescription("A test product description");
    product.setReviewIds(new ArrayList<>());

    // Act
    Product savedProduct = productRepository.save(product);

    // Assert
    assertNotNull(savedProduct.getId());
    assertEquals("Test Product", savedProduct.getName());

    // Verify retrieval
    Product retrievedProduct = productRepository.findById(savedProduct.getId()).orElse(null);
    assertNotNull(retrievedProduct);
    assertEquals("Test Product", retrievedProduct.getName());
  }

  @Test
  void testProductWithReviews() {
    // Arrange
    Product product = new Product();
    product.setName("Product with Reviews");
    product.setReviewIds(new ArrayList<>());
    product = productRepository.save(product);

    Review review1 = new Review();
    review1.setComment("Great product");
    review1.setRating(5);
    review1 = reviewRepository.save(review1);

    Review review2 = new Review();
    review2.setComment("Good value");
    review2.setRating(4);
    review2 = reviewRepository.save(review2);

    // Act
    product.getReviewIds().add(review1.getId());
    product.getReviewIds().add(review2.getId());
    product = productRepository.save(product);

    // Assert
    Product retrievedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertEquals(2, retrievedProduct.getReviewIds().size());
    assertTrue(retrievedProduct.getReviewIds().contains(review1.getId()));
    assertTrue(retrievedProduct.getReviewIds().contains(review2.getId()));

    // Verify reviews can be retrieved
    List<Review> reviews = reviewRepository.findByIdIn(retrievedProduct.getReviewIds());
    assertEquals(2, reviews.size());
  }

  @Test
  void testProductRatingCalculation() {
    // Arrange
    Product product = new Product();
    product.setName("Product for Rating");
    product.setReviewIds(new ArrayList<>());
    product = productRepository.save(product);

    Review review1 = new Review();
    review1.setRating(5);
    review1 = reviewRepository.save(review1);

    Review review2 = new Review();
    review2.setRating(3);
    review2 = reviewRepository.save(review2);

    product.getReviewIds().add(review1.getId());
    product.getReviewIds().add(review2.getId());

    // Act
    List<Review> reviews = reviewRepository.findByIdIn(product.getReviewIds());
    double avgRating = reviews.stream()
        .mapToDouble(Review::getRating)
        .average()
        .orElse(0.0);
    product.setRating(avgRating);
    product = productRepository.save(product);

    // Assert
    Product retrievedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertEquals(4.0, retrievedProduct.getRating(), 0.01);
  }

  @Test
  void testFindAllProducts() {
    // Arrange
    Product product1 = new Product();
    product1.setName("Product 1");
    productRepository.save(product1);

    Product product2 = new Product();
    product2.setName("Product 2");
    productRepository.save(product2);

    // Act
    List<Product> products = productRepository.findAll();

    // Assert
    assertEquals(2, products.size());
  }

  @Test
  void testDeleteProduct() {
    // Arrange
    Product product = new Product();
    product.setName("Product to Delete");
    product = productRepository.save(product);
    String productId = product.getId();

    // Act
    productRepository.deleteById(productId);

    // Assert
    assertFalse(productRepository.findById(productId).isPresent());
  }

  @Test
  void testProductWithCompany() {
    // Arrange
    Company company = new Company();
    company.setName("Test Company");
    company.setProducts(new ArrayList<>());
    company = companyRepository.save(company);

    Product product = new Product();
    product.setName("Company Product");
    product.setCompanyName("Test Company");
    product = productRepository.save(product);

    // Act
    company.getProducts().add(product.getId());
    company = companyRepository.save(company);

    // Assert
    Company retrievedCompany = companyRepository.findById(company.getId()).orElseThrow();
    assertTrue(retrievedCompany.getProducts().contains(product.getId()));

    Product retrievedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertEquals("Test Company", retrievedProduct.getCompanyName());
  }
}
