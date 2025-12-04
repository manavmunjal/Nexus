package com.nexus.controller;

import com.nexus.model.User;
import com.nexus.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * REST controller for managing User entities.
 * Provides endpoints to create and retrieve users.
 */
@RestController
@RequestMapping("/api/users")
public final class UserController {

  /**
   * Logger instance for UserController.
   */
  private static final Logger LOGGER =
  LoggerFactory.getLogger(UserController.class);

  /**
   * Repository for User entities.
   */
  private final UserRepository userRepository;

  /**
   * Constructs a UserController with the given repository.
   *
   * @param repository the repository for User entities
   */
  public UserController(final UserRepository repository) {
    this.userRepository = repository;
  }

  /**
   * Creates a new user.
   * Validates input before saving. Returns appropriate HTTP status
   * on success or failure.
   *
   * @param user the user object to create
   * @return ResponseEntity containing the created user or an error message
   */
  @PostMapping
  public ResponseEntity<?> createUser(@RequestBody final User user) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Received request to create user: {}",
      user != null ? user.getUsername() : null);
    }

    if (user == null || user.getUsername() == null
    || user.getUsername().isBlank()) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Invalid user data provided: {}", user);
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Invalid user data");
    }

    try {
      User saved = userRepository.save(user);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("User created successfully: {}", saved.getUsername());
      }

      return ResponseEntity.status(HttpStatus.CREATED).body(saved);

    } catch (DataAccessException dae) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Database error while saving user: {}",
        user.getUsername(), dae);
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Database error while saving user: " + dae.getMessage());

    } catch (Exception e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Unexpected error while creating user: {}",
        user.getUsername(), e);
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error: " + e.getMessage());
    }
  }

  /**
   * Retrieves all users.
   * Returns an empty list if an unexpected error occurs.
   *
   * @return ResponseEntity containing the list of users or an empty
   * list in case of error
   */
  @GetMapping
  public ResponseEntity<List<User>> getAllUsers() {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Received request to retrieve all users");
    }

    try {
      List<User> users = userRepository.findAll();

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Retrieved {} users successfully", users.size());
      }

      return ResponseEntity.ok(users);

    } catch (Exception e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Unexpected error while retrieving users", e);
      }
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(List.of());
    }
  }
}
