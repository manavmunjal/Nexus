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
  /**
   * Unique identifier for the review.
   */
  @Id
  private String id;
  /**
   * Comment text of the review.
   */
  private String comment;
  /**
   * Rating value of the review.
   */
  private double rating;

  /**
   * User who created the review.
   */
  @DBRef
  private User user;

  /**
   * Default constructor for Review.
   * Required for frameworks and serialization.
   */
  public Review() {
    // no-op
  }

  /**
   * Constructs a Review with the specified id, comment, rating, and user.
   *
   * @param idx the unique identifier for the review
   * @param cmt the comment text of the review
   * @param rtg the rating value of the review
   * @param usr the user who created the review
   */
  public Review(final String idx, final String cmt,
      final double rtg, final User usr) {
    this.id = idx;
    this.comment = cmt;
    this.rating = rtg;
    this.user = usr;
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
   * @param idx the review id to set
   */
  public void setId(final String idx) {
    this.id = idx;
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
   * @param cmt the comment text to set
   */
  public void setComment(final String cmt) {
    this.comment = cmt;
  }

  /**
   * Gets the review's rating value.
   *
   * @return the rating value
   */
  public double getRating() {
    return rating;
  }

  /**
   * Sets the review's rating value.
   *
   * @param rtg the rating value to set
   */
  public void setRating(final double rtg) {
    this.rating = rtg;
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
   * @param usr the user to set
   */
  public void setUser(final User usr) {
    this.user = usr;
  }
}
