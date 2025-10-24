package com.nexus.repository;

import com.nexus.model.Company;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository interface for Company entity operations.
 * Extends MongoRepository to provide CRUD operations for Company documents.
 */
public interface CompanyRepository extends MongoRepository<Company, String> {}
