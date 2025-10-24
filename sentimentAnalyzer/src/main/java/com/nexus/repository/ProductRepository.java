package com.nexus.repository;

import com.nexus.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository interface for Product entity operations.
 * Extends MongoRepository to provide CRUD operations for Product documents.
 */
public interface ProductRepository extends MongoRepository<Product, String> {}
