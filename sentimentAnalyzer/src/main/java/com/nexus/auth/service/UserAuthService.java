package com.nexus.auth.service;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.repository.AuthUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for user authentication operations.
 * Handles user creation, validation, and lookup operations.
 *
 * <p>This service manages user IDs without passwords. Each unique user ID
 * represents a distinct client that can access the API.</p>
 */
@Service
public class UserAuthService {

    /**
     * Repository for AuthUser persistence operations.
     */
    private final AuthUserRepository authUserRepository;

    /**
     * Constructs a UserAuthService with the required repository.
     *
     * @param authUserRepository the repository for AuthUser operations
     */
    public UserAuthService(final AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    /**
     * Creates a new authenticated user with the given user ID.
     *
     * @param userId the unique identifier for the new user
     * @return the created AuthUser entity
     * @throws IllegalArgumentException if userId is null or blank
     * @throws IllegalStateException if a user with this ID already exists
     */
    public AuthUser createUser(final String userId) {
        // Validate user ID is not null or blank
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }

        // Check if user already exists
        if (authUserRepository.existsByUserId(userId)) {
            throw new IllegalStateException("User with ID '" + userId + "' already exists");
        }

        AuthUser newUser = new AuthUser(userId);
        return authUserRepository.save(newUser);
    }

    /**
     * Validates that a user with the given ID exists.
     *
     * @param userId the user ID to validate
     * @return the AuthUser if found
     * @throws IllegalArgumentException if userId is null or blank
     * @throws IllegalStateException if no user with this ID exists
     */
    public AuthUser validateUser(final String userId) {
        // Validate user ID is not null or blank
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }

        AuthUser user = authUserRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "User with ID '" + userId + "' does not exist. "
                                + "Please call POST /api/auth/users first to create the user."));

        // Update last access time
        user.updateLastAccess();
        authUserRepository.save(user);

        return user;
    }

    /**
     * Checks if a user with the given ID exists.
     *
     * @param userId the user ID to check
     * @return true if the user exists, false otherwise
     */
    public boolean userExists(final String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return authUserRepository.existsByUserId(userId);
    }

    /**
     * Finds a user by their user ID.
     *
     * @param userId the user ID to search for
     * @return an Optional containing the user if found
     */
    public Optional<AuthUser> findByUserId(final String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return authUserRepository.findByUserId(userId);
    }

    /**
     * Retrieves all authenticated users.
     *
     * @return a list of all AuthUser entities
     */
    public List<AuthUser> getAllUsers() {
        return authUserRepository.findAll();
    }

    /**
     * Deletes a user by their user ID.
     *
     * @param userId the user ID to delete
     * @throws IllegalArgumentException if userId is null or blank
     * @throws IllegalStateException if no user with this ID exists
     */
    public void deleteUser(final String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }

        AuthUser user = authUserRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "User with ID '" + userId + "' does not exist"));

        authUserRepository.delete(user);
    }
}
