package com.nexus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a user entity in the system.
 * This class is mapped to the "users" collection in MongoDB.
 */
@Document(collection = "users")
public class User {
  @Id
  private String id;
  private String username;
  private String email;

  /**
   * Default constructor for User.
   */
  public User() {}

  /**
   * Constructs a User with the specified id, username, and email.
   *
   * @param id the unique identifier for the user
   * @param username the username of the user
   * @param email the email address of the user
   */
  public User(String id, String username, String email) {
    this.id = id;
    this.username = username;
    this.email = email;
  }

  /**
   * Gets the user's unique identifier.
   *
   * @return the user id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the user's unique identifier.
   *
   * @param id the user id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the user's username.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the user's username.
   *
   * @param username the username to set
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Gets the user's email address.
   *
   * @return the email address
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the user's email address.
   *
   * @param email the email address to set
   */
  public void setEmail(String email) {
    this.email = email;
  }
}
