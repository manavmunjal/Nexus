package com.nexus.repository;

import com.nexus.model.Company;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository interface for Company entity operations.
 * Extends MongoRepository to provide CRUD operations for Company documents.
 */
public interface CompanyRepository extends MongoRepository<Company, String> {
  /**
   * Finds companies that contain the specified product ID in their products list.
   *
   * @param productId the product ID to search for
   * @return list of companies containing the product
   */
  java.util.List<Company> findByProductsContaining(String productId);

  /**
   * Finds a company by its name.
   *
   * @param name the company name to search for
   * @return optional containing the company if found
   */
  java.util.Optional<Company> findByName(String name);
}
