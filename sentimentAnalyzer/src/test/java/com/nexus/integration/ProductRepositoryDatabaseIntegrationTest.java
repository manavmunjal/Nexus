package com.nexus.integration;

import com.nexus.model.Product;
import com.nexus.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ProductRepository using embedded/in-memory MongoDB (Flapdoodle).
 *
 * <p>This test directly interacts with the database using repository methods,
 * without calling any REST APIs.
 */
@DataMongoTest
class ProductRepositoryDatabaseIntegrationTest {

  @Autowired
  private ProductRepository productRepository;

  @AfterEach
  void tearDown() {
    productRepository.deleteAll();
  }

  @Test
  void shouldSaveAndRetrieveProducts() {
    // Create a new product
    Product product = new Product();
    product.setName("Test Product");
    product.setCompanyName("Test Company");

    // Save directly to the in-memory MongoDB
    Product savedProduct = productRepository.save(product);

    assertThat(savedProduct).isNotNull();
    assertThat(savedProduct.getId()).isNotNull();
    assertThat(savedProduct.getName()).isEqualTo("Test Product");

    // Retrieve all products from the database
    List<Product> products = productRepository.findAll();

    assertThat(products).isNotNull();
    assertThat(products).hasSize(1);
    assertThat(products.get(0).getId()).isEqualTo(savedProduct.getId());
    assertThat(products.get(0).getName()).isEqualTo("Test Product");
  }
}
