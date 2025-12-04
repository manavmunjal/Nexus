package com.nexus.auth.controller;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserAuthController.
 * Tests REST endpoints for user creation, retrieval, validation, and deletion.
 */
class UserAuthControllerTest {

    private UserAuthService userAuthService;
    private UserAuthController controller;

    @BeforeEach
    void setUp() {
        userAuthService = mock(UserAuthService.class);
        controller = new UserAuthController(userAuthService);
    }

    //  createUser tests 

    @Test
    void createUser_ShouldReturnCreatedUser_WhenUserIdIsValid() {
        // Arrange
        String userId = "user123";
        AuthUser createdUser = new AuthUser(userId);
        Map<String, String> request = Map.of("userId", userId);

        when(userAuthService.createUser(userId)).thenReturn(createdUser);

        // Act
        ResponseEntity<?> response = controller.createUser(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(createdUser, response.getBody());
        verify(userAuthService, times(1)).createUser(userId);
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenUserIdIsInvalid() {
        // Arrange
        Map<String, String> request = Map.of("userId", "");

        // Act
        ResponseEntity<?> response = controller.createUser(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("User ID is required"));
    }

    @Test
    void createUser_ShouldReturnConflict_WhenUserAlreadyExists() {
        // Arrange
        String userId = "existingUser";
        Map<String, String> request = Map.of("userId", userId);
        when(userAuthService.createUser(userId))
                .thenThrow(new IllegalStateException("User with ID 'existingUser' already exists"));

        // Act
        ResponseEntity<?> response = controller.createUser(request);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("already exists"));
    }

    @Test
    void createUser_ShouldReturnInternalServerError_OnDatabaseException() {
        // Arrange
        String userId = "user123";
        Map<String, String> request = Map.of("userId", userId);
        when(userAuthService.createUser(userId))
                .thenThrow(new DataAccessException("Database connection failed") {});

        // Act
        ResponseEntity<?> response = controller.createUser(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("Database error"));
    }

    @Test
    void createUser_ShouldReturnInternalServerError_OnUnexpectedException() {
        // Arrange
        String userId = "user123";
        Map<String, String> request = Map.of("userId", userId);
        when(userAuthService.createUser(userId))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<?> response = controller.createUser(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("Unexpected error"));
    }

    //  getAllUsers tests 

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        // Arrange
        String requesterId = "ADMIN";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        AuthUser user1 = new AuthUser("user1");
        AuthUser user2 = new AuthUser("user2");
        List<AuthUser> users = List.of(user1, user2);

        when(userAuthService.getAllUsers()).thenReturn(users);

        // Act
        ResponseEntity<?> response = controller.getAllUsers(requesterId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users, response.getBody());
    }

    @Test
    void getAllUsers_ShouldReturnEmptyList_WhenNoUsers() {
        // Arrange
        String requesterId = "ADMIN";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        when(userAuthService.getAllUsers()).thenReturn(List.of());

        // Act
        ResponseEntity<?> response = controller.getAllUsers(requesterId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(((List<?>) response.getBody()).isEmpty());
    }

    @Test
    void getAllUsers_ShouldReturnInternalServerError_OnDatabaseException() {
        // Arrange
        String requesterId = "ADMIN";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        when(userAuthService.getAllUsers())
                .thenThrow(new DataAccessException("Database error") {});

        // Act
        ResponseEntity<?> response = controller.getAllUsers(requesterId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
    }

    @Test
    void getAllUsers_ShouldReturnForbidden_WhenRequesterIsNotAdmin() {
        // Arrange
        String requesterId = "user123";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));

        // Act
        ResponseEntity<?> response = controller.getAllUsers(requesterId);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    //  getUser tests 

    @Test
    void getUser_ShouldReturnUser_WhenUserExists() {
        // Arrange
        String userId = "user123";
        AuthUser user = new AuthUser(userId);
        when(userAuthService.findByUserId(userId)).thenReturn(Optional.of(user));

        // Act
        ResponseEntity<?> response = controller.getUser(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody());
    }

    @Test
    void getUser_ShouldReturnNotFound_WhenUserDoesNotExist() {
        // Arrange
        String userId = "nonExistentUser";
        when(userAuthService.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = controller.getUser(userId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("User not found"));
        assertTrue(body.get("error").contains("POST /api/auth/users"));
    }

    @Test
    void getUser_ShouldReturnInternalServerError_OnDatabaseException() {
        // Arrange
        String userId = "user123";
        when(userAuthService.findByUserId(userId))
                .thenThrow(new DataAccessException("Database error") {});

        // Act
        ResponseEntity<?> response = controller.getUser(userId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    //  validateUser tests 

    @Test
    void validateUser_ShouldReturnValid_WhenUserExists() {
        // Arrange
        String requesterId = "ADMIN";
        String userId = "user123";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        when(userAuthService.userExists(userId)).thenReturn(true);

        // Act
        ResponseEntity<?> response = controller.validateUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(userId, body.get("userId"));
        assertEquals(true, body.get("valid"));
    }

    @Test
    void validateUser_ShouldReturnInvalid_WhenUserDoesNotExist() {
        // Arrange
        String requesterId = "ADMIN";
        String userId = "nonExistentUser";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        when(userAuthService.userExists(userId)).thenReturn(false);

        // Act
        ResponseEntity<?> response = controller.validateUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(userId, body.get("userId"));
        assertEquals(false, body.get("valid"));
    }

    @Test
    void validateUser_ShouldReturnInternalServerError_OnException() {
        // Arrange
        String requesterId = "ADMIN";
        String userId = "user123";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        when(userAuthService.userExists(userId))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<?> response = controller.validateUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void validateUser_ShouldReturnForbidden_WhenRequesterIsNotAdmin() {
        // Arrange
        String requesterId = "user123";
        String userId = "any";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));

        // Act
        ResponseEntity<?> response = controller.validateUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    //  deleteUser tests 

    @Test
    void deleteUser_ShouldReturnSuccess_WhenUserDeleted() {
        // Arrange
        String requesterId = "ADMIN";
        String userId = "user123";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        doNothing().when(userAuthService).deleteUser(userId);

        // Act
        ResponseEntity<?> response = controller.deleteUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("message").contains("deleted successfully"));
        verify(userAuthService, times(1)).deleteUser(userId);
    }

    @Test
    void deleteUser_ShouldReturnBadRequest_WhenUserIdIsInvalid() {
        // Arrange
        String requesterId = "ADMIN";
        String userId = "";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        doThrow(new IllegalArgumentException("User ID cannot be null or blank"))
                .when(userAuthService).deleteUser(userId);

        // Act
        ResponseEntity<?> response = controller.deleteUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() {
        // Arrange
        String requesterId = "ADMIN";
        String userId = "nonExistentUser";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        doThrow(new IllegalStateException("User with ID 'nonExistentUser' does not exist"))
                .when(userAuthService).deleteUser(userId);

        // Act
        ResponseEntity<?> response = controller.deleteUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("does not exist"));
    }

    @Test
    void deleteUser_ShouldReturnInternalServerError_OnDatabaseException() {
        // Arrange
        String requesterId = "ADMIN";
        String userId = "user123";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));
        doThrow(new DataAccessException("Database error") {})
                .when(userAuthService).deleteUser(userId);

        // Act
        ResponseEntity<?> response = controller.deleteUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void deleteUser_ShouldReturnForbidden_WhenRequesterIsNotAdmin() {
        // Arrange
        String requesterId = "user123"; // not ADMIN
        String userId = "victim";
        when(userAuthService.validateUser(requesterId)).thenReturn(new AuthUser(requesterId));

        // Act
        ResponseEntity<?> response = controller.deleteUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deleteUser_ShouldReturnNotFoundWithSuggestion_WhenRequesterDoesNotExist() {
        // Arrange
        String requesterId = "ghost";
        String userId = "someone";
        // Simulate validateUser throwing with suggestion
        org.mockito.Mockito.when(userAuthService.validateUser(requesterId))
                .thenThrow(new IllegalStateException(
                        "User with ID '" + requesterId + "' does not exist. " +
                                "Please call POST /api/auth/users first to create the user."));

        // Act
        ResponseEntity<?> response = controller.deleteUser(requesterId, userId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("POST /api/auth/users"));
    }
}
