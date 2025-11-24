package com.nexus.model;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a company entity in the system.
 * This class is mapped to the "companies" collection in MongoDB.
 */
@Document(collection = "companies")
public class Company {
  /**
   * Unique identifier for the company.
   */
  @Id
  private String id;
  /**
   * Name of the company.
   */
  private String name;
  /**
   * Product ids of the company.
   */
  private List<String> products;
  /**
   * Rating of the company.
   */
  private double rating;

  /**
   * Default constructor for Company.
   * Required for frameworks and serialization.
   */
  public Company() {
  }

  /**
   * Constructs a Company with the specified id and name.
   *
   * @param companyId   the unique identifier for the company
   * @param companyName the name of the company
   * @param products    the product ids of the company
   */
  public Company(final String companyId, final String companyName, final List<String> products) {
    this.id = companyId;
    this.name = companyName;
    this.products = products;
  }

  /**
   * Gets the company's unique identifier.
   *
   * @return the company id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the company's unique identifier.
   *
   * @param companyId the company id to set
   */
  public void setId(final String companyId) {
    this.id = companyId;
  }

  /**
   * Gets the company's name.
   *
   * @return the company name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the company's rating.
   *
   * @return the company rating
   */
  public double getRating() {
    return rating;
  }

  /**
   * Sets the company's name.
   *
   * @param companyName the company name to set
   */
  public void setName(final String companyName) {
    this.name = companyName;
  }

  /**
   * Sets the company's rating.
   *
   * @param rtg the company rating to set
   */
  public void setRating(final double rtg) {
    this.rating = rtg;
  }

  /**
   * Gets the company's product ids.
   *
   * @return the company product ids
   */
  public List<String> getProducts() {
    return products;
  }

  /**
   * Sets the company's product ids.
   *
   * @param products the company product ids to set
   */
  public void setProducts(final List<String> products) {
    this.products = products;
  }

  /**
   * Adds a product id to the company's product ids.
   *
   * @param productId the product id to add
   */
  public void addProductId(final String productId) {
    this.products.add(productId);
  }

  /**
   * Removes a product id from the company's product ids.
   *
   * @param productId the product id to remove
   */
  public void removeProductId(final String productId) {
    this.products.remove(productId);
  }

  /**
   * Finds average rating of company from products
   *
   * @param productList the list of products to calculate average from
   * @return the average rating of the company
   */
  public double findAverageRating(final List<Product> productList) {
    if (productList == null || productList.isEmpty()) {
      this.rating = 0.0;
      return 0.0;
    }
    this.rating = productList.stream().mapToDouble(Product::getRating).average().orElse(0.0);
    return this.rating;
  }
}
