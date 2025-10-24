package com.nexus.controller;

import com.nexus.model.User;
import com.nexus.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing User entities.
 * Provides endpoints for creating and retrieving users.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;

    /**
     * Constructs a UserController with the specified UserRepository.
     *
     * @param userRepository the repository for user operations
     */
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a new user.
     *
     * @param user the user to create
     * @return the created user, or null if an error occurs
     */
    @PostMapping
    public User createUser(@RequestBody User user) {
        try {
            return userRepository.save(user);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves all users.
     *
     * @return a list of all users, or an empty list if an error occurs
     */
    @GetMapping
    public List<User> getAllUsers() {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }
}
