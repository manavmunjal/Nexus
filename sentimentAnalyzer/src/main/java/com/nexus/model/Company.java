package com.nexus.model;

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
   * Default constructor for Company.
   * Required for frameworks and serialization.
   */
  public Company() { }

  /**
   * Constructs a Company with the specified id and name.
   *
   * @param companyId the unique identifier for the company
   * @param companyName the name of the company
   */
  public Company(final String companyId, final String companyName) {
    this.id = companyId;
    this.name = companyName;
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
   * Sets the company's name.
   *
   * @param companyName the company name to set
   */
  public void setName(final String companyName) {
    this.name = companyName;
  }
}
