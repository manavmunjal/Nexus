package com.nexus.repository;

import com.nexus.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
public class ProductRepositoryEmbeddedTest {

  @Autowired
  private ProductRepository productRepository;

  @Test
  public void testSaveAndFindById() {
    Product product = new Product();
    product.setName("Test Product");
    product.setDescription("Test Description");
    product.setCompanyName("Test Company");

    Product savedProduct = productRepository.save(product);

    assertThat(savedProduct.getId()).isNotNull();

    Optional<Product> foundProduct = productRepository.findById(savedProduct.getId());

    assertThat(foundProduct).isPresent();
    assertThat(foundProduct.get().getName()).isEqualTo("Test Product");
    assertThat(foundProduct.get().getDescription()).isEqualTo("Test Description");
  }
}
