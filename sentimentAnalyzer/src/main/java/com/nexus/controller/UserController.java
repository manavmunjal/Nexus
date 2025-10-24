package com.nexus.controller;

import com.nexus.model.User;
import com.nexus.repository.UserRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public final class UserController {

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
   * Creates a new user. Not designed for extension.
   *
   * @param user the user to create
   * @return ResponseEntity with the created user or an error message
   */
  @PostMapping
  public ResponseEntity<?> createUser(@RequestBody final User user) {
    try {
      User saved = userRepository.save(user);
      return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (DataAccessException dae) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body("Database error while saving user: "
        + dae.getMessage());
    } catch (Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body("Unexpected error: "
        + e.getMessage());
    }
  }

  /**
   * Retrieves all users.
   *
   * @return ResponseEntity with the list of users or an error message
   */
  @GetMapping
  public ResponseEntity<List<User>> getAllUsers() {
    try {
      List<User> users = userRepository.findAll();
      return ResponseEntity.ok(users);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(List.of());
    }
  }
}
