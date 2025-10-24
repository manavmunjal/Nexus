package com.nexus.controller;

import com.nexus.model.User;
import com.nexus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserController userController;

  private User testUser;

  @BeforeEach
  void setUp() {
      testUser = new User();
      testUser.setId("1");
      testUser.setUsername("testuser");
      testUser.setEmail("test@example.com");
  }

  @Test
  void createUser_ShouldSaveAndReturnUser() {
      when(userRepository.save(any(User.class))).thenReturn(testUser);

      User result = userController.createUser(testUser);

      assertNotNull(result);
      assertEquals("1", result.getId());
      assertEquals("testuser", result.getUsername());
      assertEquals("test@example.com", result.getEmail());
      verify(userRepository).save(testUser);
  }

  @Test
  void getAllUsers_ShouldReturnListOfUsers() {
      List<User> users = List.of(testUser);
      when(userRepository.findAll()).thenReturn(users);

      List<User> result = userController.getAllUsers();

      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(testUser.getId(), result.get(0).getId());
      verify(userRepository).findAll();
  }
}