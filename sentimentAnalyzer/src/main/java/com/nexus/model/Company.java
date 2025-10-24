package com.nexus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a company entity in the system.
 * This class is mapped to the "companies" collection in MongoDB.
 */
@Document(collection = "companies")
public class Company {
  @Id
  private String id;
  private String name;

  /**
   * Default constructor for Company.
   */
  public Company() {}

  /**
   * Constructs a Company with the specified id and name.
   *
   * @param id the unique identifier for the company
   * @param name the name of the company
   */
  public Company(String id, String name) {
  this.id = id;
  this.name = name;
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
   * @param id the company id to set
   */
  public void setId(String id) {
  this.id = id;
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
   * Sets the company's name.
   *
   * @param name the company name to set
   */
  public void setName(String name) {
  this.name = name;
  }
}
