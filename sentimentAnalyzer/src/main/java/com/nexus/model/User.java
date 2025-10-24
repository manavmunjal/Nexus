package com.nexus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a user entity in the system.
 * This class is mapped to the "users" collection in MongoDB.
 */
@Document(collection = "users")
public class User {
  /**
   * Unique identifier for the user.
   */
  @Id
  private String id;
  /**
   * Username of the user.
   */
  private String username;
  /**
   * Email address of the user.
   */
  private String email;

  /**
   * Default constructor for User.
   * Required for frameworks and serialization.
   */
  public User() { }

  /**
   * Constructs a User with the specified id, username, and email.
   *
   * @param idx the unique identifier for the user
   * @param uname the username of the user
   * @param mail the email address of the user
   */
  public User(final String idx, final String uname, final String mail) {
    this.id = idx;
    this.username = uname;
    this.email = mail;
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
   * @param idx the user id to set
   */
  public void setId(final String idx) {
    this.id = idx;
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
   * @param uname the username to set
   */
  public void setUsername(final String uname) {
    this.username = uname;
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
   * @param mail the email address to set
   */
  public void setEmail(final String mail) {
    this.email = mail;
  }
}
