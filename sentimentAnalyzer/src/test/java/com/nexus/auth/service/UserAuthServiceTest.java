package com.nexus.auth.service;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserAuthService.
 * Tests user creation, validation, lookup, and deletion operations.
 */
class UserAuthServiceTest {

    private AuthUserRepository authUserRepository;
    private UserAuthService userAuthService;

    @BeforeEach
    void setUp() {
        authUserRepository = mock(AuthUserRepository.class);
        userAuthService = new UserAuthService(authUserRepository);
    }

    //  createUser tests 

    @Test
    void createUser_ShouldCreateAndReturnUser_WhenUserIdIsValid() {
        // Arrange
        String userId = "user123";
        AuthUser savedUser = new AuthUser(userId);
        savedUser.setId("mongo-id-123");

        when(authUserRepository.existsByUserId(userId)).thenReturn(false);
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(savedUser);

        // Act
        AuthUser result = userAuthService.createUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(authUserRepository, times(1)).existsByUserId(userId);
        verify(authUserRepository, times(1)).save(any(AuthUser.class));
    }

    @Test
    void createUser_ShouldThrowException_WhenUserIdIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userAuthService.createUser(null)
        );

        assertEquals("User ID cannot be null or blank", exception.getMessage());
        verify(authUserRepository, never()).save(any());
    }

    @Test
    void createUser_ShouldThrowException_WhenUserIdIsBlank() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userAuthService.createUser("   ")
        );

        assertEquals("User ID cannot be null or blank", exception.getMessage());
        verify(authUserRepository, never()).save(any());
    }

    @Test
    void createUser_ShouldThrowException_WhenUserIdIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userAuthService.createUser("")
        );

        assertEquals("User ID cannot be null or blank", exception.getMessage());
        verify(authUserRepository, never()).save(any());
    }

    @Test
    void createUser_ShouldThrowException_WhenUserAlreadyExists() {
        // Arrange
        String userId = "existingUser";
        when(authUserRepository.existsByUserId(userId)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userAuthService.createUser(userId)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(authUserRepository, never()).save(any());
    }

    //  validateUser tests 

    @Test
    void validateUser_ShouldReturnUser_WhenUserExists() {
        // Arrange
        String userId = "user123";
        AuthUser existingUser = new AuthUser(userId);

        when(authUserRepository.findByUserId(userId)).thenReturn(Optional.of(existingUser));
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(existingUser);

        // Act
        AuthUser result = userAuthService.validateUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(authUserRepository, times(1)).findByUserId(userId);
        verify(authUserRepository, times(1)).save(existingUser); // Last access updated
    }

    @Test
    void validateUser_ShouldThrowException_WhenUserIdIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userAuthService.validateUser(null)
        );

        assertEquals("User ID cannot be null or blank", exception.getMessage());
        verify(authUserRepository, never()).findByUserId(any());
    }

    @Test
    void validateUser_ShouldThrowException_WhenUserIdIsBlank() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userAuthService.validateUser("  ")
        );

        assertEquals("User ID cannot be null or blank", exception.getMessage());
        verify(authUserRepository, never()).findByUserId(any());
    }

    @Test
    void validateUser_ShouldThrowException_WhenUserDoesNotExist() {
        // Arrange
        String userId = "nonExistentUser";
        when(authUserRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userAuthService.validateUser(userId)
        );

        assertTrue(exception.getMessage().contains("does not exist"));
        assertTrue(exception.getMessage().contains("POST /api/auth/users"));
    }

    //  userExists tests 

    @Test
    void userExists_ShouldReturnTrue_WhenUserExists() {
        // Arrange
        String userId = "user123";
        when(authUserRepository.existsByUserId(userId)).thenReturn(true);

        // Act
        boolean result = userAuthService.userExists(userId);

        // Assert
        assertTrue(result);
        verify(authUserRepository, times(1)).existsByUserId(userId);
    }

    @Test
    void userExists_ShouldReturnFalse_WhenUserDoesNotExist() {
        // Arrange
        String userId = "nonExistentUser";
        when(authUserRepository.existsByUserId(userId)).thenReturn(false);

        // Act
        boolean result = userAuthService.userExists(userId);

        // Assert
        assertFalse(result);
    }

    @Test
    void userExists_ShouldReturnFalse_WhenUserIdIsNull() {
        // Act
        boolean result = userAuthService.userExists(null);

        // Assert
        assertFalse(result);
        verify(authUserRepository, never()).existsByUserId(any());
    }

    @Test
    void userExists_ShouldReturnFalse_WhenUserIdIsBlank() {
        // Act
        boolean result = userAuthService.userExists("   ");

        // Assert
        assertFalse(result);
        verify(authUserRepository, never()).existsByUserId(any());
    }

    //  findByUserId tests 

    @Test
    void findByUserId_ShouldReturnUser_WhenUserExists() {
        // Arrange
        String userId = "user123";
        AuthUser user = new AuthUser(userId);
        when(authUserRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        // Act
        Optional<AuthUser> result = userAuthService.findByUserId(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
    }

    @Test
    void findByUserId_ShouldReturnEmpty_WhenUserDoesNotExist() {
        // Arrange
        String userId = "nonExistentUser";
        when(authUserRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        Optional<AuthUser> result = userAuthService.findByUserId(userId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void findByUserId_ShouldReturnEmpty_WhenUserIdIsNull() {
        // Act
        Optional<AuthUser> result = userAuthService.findByUserId(null);

        // Assert
        assertFalse(result.isPresent());
        verify(authUserRepository, never()).findByUserId(any());
    }

    @Test
    void findByUserId_ShouldReturnEmpty_WhenUserIdIsBlank() {
        // Act
        Optional<AuthUser> result = userAuthService.findByUserId("  ");

        // Assert
        assertFalse(result.isPresent());
        verify(authUserRepository, never()).findByUserId(any());
    }

    //  getAllUsers tests 

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Arrange
        AuthUser user1 = new AuthUser("user1");
        AuthUser user2 = new AuthUser("user2");
        List<AuthUser> users = List.of(user1, user2);

        when(authUserRepository.findAll()).thenReturn(users);

        // Act
        List<AuthUser> result = userAuthService.getAllUsers();

        // Assert
        assertEquals(2, result.size());
        verify(authUserRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_ShouldReturnEmptyList_WhenNoUsers() {
        // Arrange
        when(authUserRepository.findAll()).thenReturn(List.of());

        // Act
        List<AuthUser> result = userAuthService.getAllUsers();

        // Assert
        assertTrue(result.isEmpty());
    }

    //  deleteUser tests 

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        // Arrange
        String userId = "user123";
        AuthUser user = new AuthUser(userId);
        when(authUserRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        // Act
        userAuthService.deleteUser(userId);

        // Assert
        verify(authUserRepository, times(1)).delete(user);
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserIdIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userAuthService.deleteUser(null)
        );

        assertEquals("User ID cannot be null or blank", exception.getMessage());
        verify(authUserRepository, never()).delete(any());
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserIdIsBlank() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userAuthService.deleteUser("  ")
        );

        assertEquals("User ID cannot be null or blank", exception.getMessage());
        verify(authUserRepository, never()).delete(any());
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserDoesNotExist() {
        // Arrange
        String userId = "nonExistentUser";
        when(authUserRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userAuthService.deleteUser(userId)
        );

        assertTrue(exception.getMessage().contains("does not exist"));
        verify(authUserRepository, never()).delete(any());
    }
}
