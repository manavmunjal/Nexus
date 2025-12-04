package com.nexus.auth.integration;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.repository.AuthUserRepository;
import com.nexus.auth.service.UserAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for user authentication flow.
 * Tests the complete workflow of user creation, validation, and API access.
 */
@ExtendWith(MockitoExtension.class)
class UserAuthIntegrationTest {

    @Mock
    private AuthUserRepository authUserRepository;

    private UserAuthService userAuthService;

    @BeforeEach
    void setUp() {
        userAuthService = new UserAuthService(authUserRepository);
    }

    /**
     * Tests the complete user registration and validation flow.
     */
    @Test
    void testCompleteUserRegistrationAndValidationFlow() {
        // Arrange - Create new user
        String userId = "integration-test-user";
        AuthUser newUser = new AuthUser(userId);
        newUser.setId("mongo-generated-id");

        when(authUserRepository.existsByUserId(userId)).thenReturn(false);
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(newUser);

        // Act - Create user
        AuthUser createdUser = userAuthService.createUser(userId);

        // Assert - User created successfully
        assertNotNull(createdUser);
        assertEquals(userId, createdUser.getUserId());
        assertNotNull(createdUser.getCreatedAt());
        verify(authUserRepository, times(1)).save(any(AuthUser.class));

        // Arrange - Setup for validation
        when(authUserRepository.findByUserId(userId)).thenReturn(Optional.of(newUser));

        // Act - Validate the user
        AuthUser validatedUser = userAuthService.validateUser(userId);

        // Assert - User validated and last access updated
        assertNotNull(validatedUser);
        assertEquals(userId, validatedUser.getUserId());
        verify(authUserRepository, times(2)).save(any(AuthUser.class)); // Once for create, once for validation
    }

    /**
     * Tests that duplicate user creation is prevented.
     */
    @Test
    void testDuplicateUserCreationPrevented() {
        // Arrange
        String userId = "existing-user";
        when(authUserRepository.existsByUserId(userId)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userAuthService.createUser(userId)
        );

        assertTrue(exception.getMessage().contains("already exists"));
    }

    /**
     * Tests that unauthenticated users cannot access protected resources.
     */
    @Test
    void testUnauthenticatedUserAccessDenied() {
        // Arrange
        String nonExistentUserId = "non-existent-user";
        when(authUserRepository.findByUserId(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userAuthService.validateUser(nonExistentUserId)
        );

        assertTrue(exception.getMessage().contains("does not exist"));
        assertTrue(exception.getMessage().contains("POST /api/auth/users"));
    }

    /**
     * Tests the complete user lifecycle: create, validate multiple times, delete.
     */
    @Test
    void testCompleteUserLifecycle() {
        // Arrange
        String userId = "lifecycle-test-user";
        AuthUser user = new AuthUser(userId);
        user.setId("mongo-id");

        // Step 1: Create user
        when(authUserRepository.existsByUserId(userId)).thenReturn(false);
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(user);

        AuthUser created = userAuthService.createUser(userId);
        assertNotNull(created);

        // Step 2: Multiple validations (simulating multiple API calls)
        when(authUserRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        for (int i = 0; i < 3; i++) {
            AuthUser validated = userAuthService.validateUser(userId);
            assertNotNull(validated);
        }

        // Verify save was called 4 times: 1 create + 3 validations
        verify(authUserRepository, times(4)).save(any(AuthUser.class));

        // Step 3: Check user exists
        when(authUserRepository.existsByUserId(userId)).thenReturn(true);
        assertTrue(userAuthService.userExists(userId));

        // Step 4: Delete user
        userAuthService.deleteUser(userId);
        verify(authUserRepository, times(1)).delete(user);
    }

    /**
     * Tests handling of multiple concurrent users.
     */
    @Test
    void testMultipleUsersIndependentAccess() {
        // Arrange - Create multiple users
        String userId1 = "user-alpha";
        String userId2 = "user-beta";

        AuthUser user1 = new AuthUser(userId1);
        user1.setId("id-1");
        AuthUser user2 = new AuthUser(userId2);
        user2.setId("id-2");

        when(authUserRepository.existsByUserId(userId1)).thenReturn(false);
        when(authUserRepository.existsByUserId(userId2)).thenReturn(false);
        when(authUserRepository.save(any(AuthUser.class)))
                .thenReturn(user1)
                .thenReturn(user2);

        // Act - Create both users
        AuthUser created1 = userAuthService.createUser(userId1);
        AuthUser created2 = userAuthService.createUser(userId2);

        // Assert - Both users created independently
        assertNotNull(created1);
        assertNotNull(created2);
        assertEquals(userId1, created1.getUserId());
        assertEquals(userId2, created2.getUserId());

        // Verify both can be validated independently
        when(authUserRepository.findByUserId(userId1)).thenReturn(Optional.of(user1));
        when(authUserRepository.findByUserId(userId2)).thenReturn(Optional.of(user2));

        AuthUser validated1 = userAuthService.validateUser(userId1);
        AuthUser validated2 = userAuthService.validateUser(userId2);

        assertNotNull(validated1);
        assertNotNull(validated2);
        assertEquals(userId1, validated1.getUserId());
        assertEquals(userId2, validated2.getUserId());
    }

    /**
     * Tests that getAllUsers returns all registered users.
     */
    @Test
    void testGetAllUsersReturnsCompleteList() {
        // Arrange
        AuthUser user1 = new AuthUser("user1");
        AuthUser user2 = new AuthUser("user2");
        AuthUser user3 = new AuthUser("user3");
        List<AuthUser> allUsers = List.of(user1, user2, user3);

        when(authUserRepository.findAll()).thenReturn(allUsers);

        // Act
        List<AuthUser> result = userAuthService.getAllUsers();

        // Assert
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getUserId().equals("user1")));
        assertTrue(result.stream().anyMatch(u -> u.getUserId().equals("user2")));
        assertTrue(result.stream().anyMatch(u -> u.getUserId().equals("user3")));
    }

    /**
     * Tests the findByUserId returns correct user.
     */
    @Test
    void testFindByUserIdReturnsCorrectUser() {
        // Arrange
        String userId = "findable-user";
        AuthUser user = new AuthUser(userId);
        user.setId("mongo-id");

        when(authUserRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(authUserRepository.findByUserId("other-user")).thenReturn(Optional.empty());

        // Act & Assert - Found user
        Optional<AuthUser> found = userAuthService.findByUserId(userId);
        assertTrue(found.isPresent());
        assertEquals(userId, found.get().getUserId());

        // Act & Assert - Not found user
        Optional<AuthUser> notFound = userAuthService.findByUserId("other-user");
        assertFalse(notFound.isPresent());
    }

    /**
     * Tests that deleting a non-existent user throws appropriate exception.
     */
    @Test
    void testDeleteNonExistentUserThrowsException() {
        // Arrange
        String nonExistentUserId = "ghost-user";
        when(authUserRepository.findByUserId(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userAuthService.deleteUser(nonExistentUserId)
        );

        assertTrue(exception.getMessage().contains("does not exist"));
    }
}
