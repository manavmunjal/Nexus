package com.nexus.auth.repository;

import com.nexus.auth.model.AuthUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repository interface for AuthUser entity operations.
 * Provides CRUD operations and custom queries for user authentication.
 */
public interface AuthUserRepository extends MongoRepository<AuthUser, String> {

    /**
     * Finds an authenticated user by their unique user ID.
     *
     * @param userId the user ID to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<AuthUser> findByUserId(String userId);

    /**
     * Checks if a user with the given user ID exists.
     *
     * @param userId the user ID to check
     * @return true if a user with this ID exists, false otherwise
     */
    boolean existsByUserId(String userId);
}
