package com.nexus.integration;

import com.nexus.model.User;
import com.nexus.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for UserRepository using embedded/in-memory MongoDB (Flapdoodle).
 *
 * <p>Tests saving, retrieving, and querying User documents directly using the repository.
 */
@DataMongoTest
class UserRepositoryDatabaseIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  /**
   * Tests saving a user and retrieving it by ID.
   */
  @Test
  void shouldSaveAndRetrieveUser() {
    User user = new User();
    user.setUsername("johndoe");
    user.setEmail("johndoe@example.com");

    User savedUser = userRepository.save(user);

    assertThat(savedUser).isNotNull();
    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getUsername()).isEqualTo("johndoe");
    assertThat(savedUser.getEmail()).isEqualTo("johndoe@example.com");

    Optional<User> fetched = userRepository.findById(savedUser.getId());
    assertThat(fetched).isPresent();
    assertThat(fetched.get().getUsername()).isEqualTo("johndoe");
    assertThat(fetched.get().getEmail()).isEqualTo("johndoe@example.com");
  }

  /**
   * Tests finding a user by their username.
   */
  @Test
  void shouldFindUserByUsername() {
    User user = new User();
    user.setUsername("alice");
    user.setEmail("alice@example.com");

    userRepository.save(user);

    Optional<User> found = userRepository.findByUsername("alice");

    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo("alice");
    assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
  }

  /**
   * Tests that searching for a non-existent username returns empty.
   */
  @Test
  void shouldReturnEmptyWhenUsernameNotFound() {
    Optional<User> found = userRepository.findByUsername("nonexistent");
    assertThat(found).isEmpty();
  }
}
