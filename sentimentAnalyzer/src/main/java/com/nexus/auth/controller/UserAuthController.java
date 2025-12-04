package com.nexus.auth.controller;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * REST controller for user authentication operations.
 * Provides endpoints to create, retrieve, and manage user IDs.
 *
 * <p>Users must be created via this controller before making requests
 * to other API endpoints that require a userId parameter.</p>
 */
@RestController
@RequestMapping("/api/auth/users")
public final class UserAuthController {

    /**
     * The user ID that has admin privileges for destructive operations.
     */
    private static final String ADMIN_USER_ID = "ADMIN";

    /**
     * Service for user authentication operations.
     */
    private final UserAuthService userAuthService;

    /**
     * Constructs a UserAuthController with the required service.
     *
     * @param newUserAuthService the service for user authentication operations
     */
    public UserAuthController(final UserAuthService newUserAuthService) {
        this.userAuthService = newUserAuthService;
    }

    /**
     * Creates a new authenticated user.
     * The request body should contain a "userId" field.
     *
     * @param request the request body containing the userId
     * @return ResponseEntity with the created user or error message
     */
    @PostMapping
    public ResponseEntity<?> createUser(
        @RequestBody(required = false) final Map<String, String> request) {
        try {
            if (request == null || request.get("userId") == null
            || request.get("userId").isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                        "User ID is required"));
            }
            String userId = request.get("userId");
            AuthUser created = userAuthService.createUser(userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (IllegalArgumentException iae) {
            // Invalid input (null/blank userId)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", iae.getMessage()));

        } catch (IllegalStateException ise) {
            // User already exists
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ise.getMessage()));

        } catch (DataAccessException dae) {
            // Database error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                    "Database error: " + dae.getMessage()));

        } catch (Exception e) {
            // Unexpected error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                    "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Retrieves all authenticated users.
     *
     * @param requesterId the user ID of the requester
     * @return ResponseEntity with list of all users
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @org.springframework.web.bind.annotation.RequestHeader("X-User-Id")
            final String requesterId) {
        try {
            // Validate requester exists
            userAuthService.validateUser(requesterId);

            // Check admin privileges
            if (!ADMIN_USER_ID.equals(requesterId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            List<AuthUser> users = userAuthService.getAllUsers();
            return ResponseEntity.ok(users);

        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                     "Database error: " + dae.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                     "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Retrieves a specific user by their user ID.
     *
     * @param userId the user ID to look up
     * @return ResponseEntity with the user or error message
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable final String userId) {
        try {
            return userAuthService.findByUserId(userId)
                    .map(user -> ResponseEntity.ok((Object) user))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "error",
                        "User not found: " + userId
                            + ". Please call POST /api/auth/users"
                            + " to create the user.")));

        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                     "Database error: " + dae.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                     "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Validates if a user exists.
     *
     * @param requesterId the user ID of the requester
     * @param userId the user ID to validate
     * @return ResponseEntity indicating whether the user exists
     */
    @GetMapping("/{userId}/validate")
    public ResponseEntity<?> validateUser(
            @org.springframework.web.bind.annotation.RequestHeader("X-User-Id")
            final String requesterId,
            @PathVariable final String userId) {
        try {
            // Validate requester exists
            userAuthService.validateUser(requesterId);

            // Check admin privileges
            if (!ADMIN_USER_ID.equals(requesterId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            boolean exists = userAuthService.userExists(userId);
            return ResponseEntity.ok(Map.of("userId", userId, "valid", exists));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                     "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Deletes a user by their user ID.
     *
     * @param requesterId the user ID of the requester
     * @param userId the user ID to delete
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @org.springframework.web.bind.annotation.RequestHeader("X-User-Id")
            final String requesterId,
            @PathVariable final String userId) {
        try {
            // Validate requester exists
            userAuthService.validateUser(requesterId);

            // Check admin privileges
            if (!ADMIN_USER_ID.equals(requesterId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            userAuthService.deleteUser(userId);
            return ResponseEntity.ok(Map.of("message",
             "User deleted successfully"));

        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", iae.getMessage()));

        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ise.getMessage()));

        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                     "Database error: " + dae.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                     "Unexpected error: " + e.getMessage()));
        }
    }
}
