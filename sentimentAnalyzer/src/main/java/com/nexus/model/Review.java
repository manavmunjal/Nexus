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
}
