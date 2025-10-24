package com.nexus.controller;

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
    User user = new User();
    when(userRepository.save(user)).thenThrow(new DataAccessException("DB down") {});

    ResponseEntity<?> response = userController.createUser(user);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Database error"));
  }

  @Test
  void createUser_ShouldReturnInternalServerError_OnUnexpectedException() {
    User user = new User();
    when(userRepository.save(user)).thenThrow(new RuntimeException("Unexpected"));

    ResponseEntity<?> response = userController.createUser(user);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().toString().contains("Unexpected error"));
  }

  @Test
  void getAllUsers_ShouldReturnEmptyList_OnException() {
    when(userRepository.findAll()).thenThrow(new RuntimeException("DB down"));

    ResponseEntity<List<User>> response = userController.getAllUsers();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(0, response.getBody().size());
  }
}
