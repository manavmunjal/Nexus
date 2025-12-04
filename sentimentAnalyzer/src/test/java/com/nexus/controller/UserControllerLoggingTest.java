package com.nexus.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexus.model.User;
import com.nexus.repository.UserRepository;

@ExtendWith(OutputCaptureExtension.class)
class UserControllerLoggingTest {

  private UserRepository userRepository;
  private UserController controller;
  private User user;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    controller = new UserController(userRepository);

    user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");
  }

  @Test
  void createUser_ShouldLogInfo_WhenRequestReceived(CapturedOutput output) {
    when(userRepository.save(user)).thenReturn(user);

    controller.createUser(user);

    assertTrue(output.getOut().contains("Received request to create user"), "Should log info message");
  }

  @Test
  void createUser_ShouldLogError_WhenDatabaseException(CapturedOutput output) {
    when(userRepository.save(user)).thenThrow(new DataAccessException("DB down") {
    });

    controller.createUser(user);

    assertTrue(output.getOut().contains("Database error while saving user"), "Should log error message");
  }
}
