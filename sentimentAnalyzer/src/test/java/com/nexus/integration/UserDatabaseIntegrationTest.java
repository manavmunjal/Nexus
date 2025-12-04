package com.nexus.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.controller.UserController;
import com.nexus.model.User;
import com.nexus.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests verifying Database Interactions via Mocks for UserController.
 */
@ExtendWith(MockitoExtension.class)
public class UserDatabaseIntegrationTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserController userController;

  @Test
  public void testCreateUser_CallsDatabaseSave() {
    User user = new User();
    user.setUsername("testuser");

    when(userRepository.save(any(User.class))).thenReturn(user);

    userController.createUser(user);

    verify(userRepository, times(1)).save(user);
  }

  @Test
  public void testDatabaseFailure_Returns500Error() {
    User user = new User();
    user.setUsername("testuser");

    when(userRepository.save(any(User.class)))
        .thenThrow(new DataAccessException("Connection refused") {
        });

    ResponseEntity<?> response = userController.createUser(user);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    verify(userRepository, times(1)).save(user);
  }
}
