package com.nexus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Document(collection = "reviews")
public class Review {
  @Id
  private String id;
  private String comment;
  private int rating;

  @DBRef
  private User user;

  public Review() {}

  public Review(String id, String comment, int rating, User user) {
    this.id = id;
    this.comment = comment;
    this.rating = rating;
    this.user = user;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public int getRating() {
    return rating;
  }

  public void setRating(int rating) {
    this.rating = rating;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
