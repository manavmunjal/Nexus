package com.nexus.repository;

import com.nexus.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository interface for User entity operations.
 * Extends MongoRepository to provide CRUD operations for User documents.
 */
public interface UserRepository extends MongoRepository<User, String> {
  /**
   * Finds a user by their username.
   *
   * @param username the username to search for
   * @return the user with the given username, or null if not found
   */
  java.util.Optional<User> findByUsername(String username);
}
