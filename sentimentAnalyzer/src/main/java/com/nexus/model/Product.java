package com.nexus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Represents a product entity in the system.
 * This class is mapped to the "products" collection in MongoDB.
 */
@Document(collection = "products")
public class Product {
  @Id
  private String id;
  private String name;
  private String description;
  
  /**
   * List of review IDs associated with this product.
   * Holds review IDs to avoid embedding full Review objects.
   */
  private List<String> reviewIds;

  /**
   * Default constructor for Product.
   */
  public Product() {
  }

  /**
   * Constructs a Product with the specified id, name, and description.
   *
   * @param id the unique identifier for the product
   * @param name the name of the product
   * @param description the description of the product
   */
  public Product(String id, String name, String description) {
  this.id = id;
  this.name = name;
  this.description = description;
  }

  /**
   * Gets the product's unique identifier.
   *
   * @return the product id
   */
  public String getId() {
  return id;
  }

  /**
   * Sets the product's unique identifier.
   *
   * @param id the product id to set
   */
  public void setId(String id) {
  this.id = id;
  }

  /**
   * Gets the product's name.
   *
   * @return the product name
   */
  public String getName() {
  return name;
  }

  /**
   * Sets the product's name.
   *
   * @param name the product name to set
   */
  public void setName(String name) {
  this.name = name;
  }

  /**
   * Gets the product's description.
   *
   * @return the product description
   */
  public String getDescription() {
  return description;
  }

  /**
   * Sets the product's description.
   *
   * @param description the product description to set
   */
  public void setDescription(String description) {
  this.description = description;
  }

  /**
   * Gets the list of review IDs associated with this product.
   *
   * @return the list of review IDs
   */
  public List<String> getReviewIds() {
  return reviewIds;
  }

  /**
   * Sets the list of review IDs associated with this product.
   *
   * @param reviewIds the list of review IDs to set
   */
  public void setReviewIds(List<String> reviewIds) {
  this.reviewIds = reviewIds;
  }
}
