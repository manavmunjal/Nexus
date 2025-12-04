package com.nexus.repository;

import com.nexus.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
public class UserRepositoryEmbeddedTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  public void testSaveAndFindByUsername() {
    User user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");

    User savedUser = userRepository.save(user);

    assertThat(savedUser.getId()).isNotNull();

    Optional<User> foundUser = userRepository.findByUsername("testuser");

    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
  }
}
