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
  /**
   * Unique identifier for the product.
   */
  @Id
  private String id;
  /**
   * Name of the product.
   */
  private String name;
  /**
   * Description of the product.
   */
  private String description;
  /**
   * List of review IDs associated with this product.
   * Holds review IDs to avoid embedding full Review objects.
   */
  private List<String> reviewIds;

  /**
   * Default constructor for Product.
   * Required for frameworks and serialization.
   */
  public Product() {
  }

  /**
   * Constructs a Product with the specified id, name, and description.
   *
   * @param pId the unique identifier for the product
   * @param pN the name of the product
   * @param pD the description of the product
   */
  public Product(final String pId, final String pN, final String pD) {
    this.id = pId;
    this.name = pN;
    this.description = pD;
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
   * @param pId the product id to set
   */
  public void setId(final String pId) {
    this.id = pId;
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
   * @param pN the product name to set
   */
  public void setName(final String pN) {
    this.name = pN;
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
   * @param pD the product description to set
   */
  public void setDescription(final String pD) {
    this.description = pD;
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
   * @param rIds the list of review IDs to set
   */
  public void setReviewIds(final List<String> rIds) {
    this.reviewIds = rIds;
  }
}
