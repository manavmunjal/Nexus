package com.nexus.controller;

import java.util.ArrayList;
import com.nexus.model.User;
import com.nexus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

  private UserRepository userRepository;
  private UserController userController;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    userController = new UserController(userRepository);
  }

  @Test
  void createUser_ShouldReturnInternalServerError_OnDatabaseException() {
    // Arrange
    User user = new User();
    user.setUsername("testuser");
    when(userRepository.save(user)).thenThrow(new DataAccessException("DB down") {});

    // Act
    ResponseEntity<?> response = userController.createUser(user);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void createUser_ShouldReturnInternalServerError_OnUnexpectedException() {
    // Arrange
    User user = new User();
    user.setUsername("testuser");
    when(userRepository.save(user)).thenThrow(new RuntimeException("Unexpected"));

    // Act
    ResponseEntity<?> response = userController.createUser(user);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Unexpected error"));
  }

  @Test
  void getAllUsers_ShouldReturnEmptyList_OnException() {
    // Arrange
    when(userRepository.findAll()).thenThrow(new RuntimeException("DB down"));

    // Act
    ResponseEntity<List<User>> response = userController.getAllUsers();

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(0, response.getBody().size());
  }

  @Test
  void createUser_ShouldReturnBadRequest_WhenUserIsNull() {
    // Arrange
    // No user object passed in (null)

    // Act
    ResponseEntity<?> response = userController.createUser(null);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Invalid user data"));
    verify(userRepository, never()).save(any());
  }

  @Test
  void createUser_ShouldReturnBadRequest_WhenUsernameIsBlank() {
    // Arrange
    User user = new User();
    user.setUsername("  "); // blank username

    // Act
    ResponseEntity<?> response = userController.createUser(user);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Invalid user data"));
    verify(userRepository, never()).save(any());
  }

  @Test
  void createUser_ShouldReturnCreated_WhenValidUser() {
    // Arrange
    User user = new User();
    user.setUsername("Alice");
    when(userRepository.save(user)).thenReturn(user);

    // Act
    ResponseEntity<?> response = userController.createUser(user);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(user, response.getBody());
    verify(userRepository, times(1)).save(user);
  }

  @Test
  void getAllUsers_ShouldReturnUsers_WhenRepositoryHasData() {
    // Arrange
    User user1 = new User();
    user1.setUsername("Alice");
    User user2 = new User();
    user2.setUsername("Bob");

    List<User> users = new ArrayList<>();
    users.add(user1);
    users.add(user2);

    when(userRepository.findAll()).thenReturn(users);

    // Act
    ResponseEntity<List<User>> response = userController.getAllUsers();

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(2, response.getBody().size());
    assertEquals("Alice", response.getBody().get(0).getUsername());
    assertEquals("Bob", response.getBody().get(1).getUsername());
  }

  @Test
  void createMultipleUsers_ShouldDistinguishBetweenThem() {
    // Arrange
    User user1 = new User();
    user1.setUsername("Alice");
    User user2 = new User();
    user2.setUsername("Bob");

    when(userRepository.save(user1)).thenReturn(user1);
    when(userRepository.save(user2)).thenReturn(user2);

    // Act
    ResponseEntity<?> response1 = userController.createUser(user1);
    ResponseEntity<?> response2 = userController.createUser(user2);

    // Assert
    assertEquals(HttpStatus.CREATED, response1.getStatusCode());
    assertEquals(HttpStatus.CREATED, response2.getStatusCode());
    assertEquals("Alice", ((User)response1.getBody()).getUsername());
    assertEquals("Bob", ((User)response2.getBody()).getUsername());

    verify(userRepository, times(1)).save(user1);
    verify(userRepository, times(1)).save(user2);
  }
}
