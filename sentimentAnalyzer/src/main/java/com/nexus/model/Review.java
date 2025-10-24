package com.nexus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a review entity in the system.
 * This class is mapped to the "reviews" collection in MongoDB.
 */
@Document(collection = "reviews")
public class Review {
  @Id
  private String id;
  private String comment;
  private int rating;

  @DBRef
  private User user;

  /**
   * Default constructor for Review.
   */
  public Review() {}

  /**
   * Constructs a Review with the specified id, comment, rating, and user.
   *
   * @param id the unique identifier for the review
   * @param comment the comment text of the review
   * @param rating the rating value of the review
   * @param user the user who created the review
   */
  public Review(String id, String comment, int rating, User user) {
  this.id = id;
  this.comment = comment;
  this.rating = rating;
  this.user = user;
  }

  /**
   * Gets the review's unique identifier.
   *
   * @return the review id
   */
  public String getId() {
  return id;
  }

  /**
   * Sets the review's unique identifier.
   *
   * @param id the review id to set
   */
  public void setId(String id) {
  this.id = id;
  }

  /**
   * Gets the review's comment text.
   *
   * @return the comment text
   */
  public String getComment() {
  return comment;
  }

  /**
   * Sets the review's comment text.
   *
   * @param comment the comment text to set
   */
  public void setComment(String comment) {
  this.comment = comment;
  }

  /**
   * Gets the review's rating value.
   *
   * @return the rating value
   */
  public int getRating() {
  return rating;
  }

  /**
   * Sets the review's rating value.
   *
   * @param rating the rating value to set
   */
  public void setRating(int rating) {
  this.rating = rating;
  }

  /**
   * Gets the user who created the review.
   *
   * @return the user
   */
  public User getUser() {
  return user;
  }

  /**
   * Sets the user who created the review.
   *
   * @param user the user to set
   */
  public void setUser(User user) {
  this.user = user;
  }
}
