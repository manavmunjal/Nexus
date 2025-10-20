package com.nexus.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
// ...existing code...

@Document(collection = "products")
public class Product {
  @Id
  private String id;
  private String name;
  private String description;
  private List<String> reviewIds;
  /** Hold review ids to avoid embedding full Review objects. */

  public Product() {
  }

  public Product(String id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getReviewIds() {
    return reviewIds;
  }

  public void setReviewIds(List<String> reviewIds) {
    this.reviewIds = reviewIds;
  }
}
