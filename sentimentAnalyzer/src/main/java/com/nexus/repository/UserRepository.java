package com.nexus.repository;

import com.nexus.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository interface for User entity operations.
 * Extends MongoRepository to provide CRUD operations for User documents.
 */
public interface UserRepository extends MongoRepository<User, String> {}

