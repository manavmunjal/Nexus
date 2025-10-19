package com.nexus.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "products")
public class Product {
  @Id
  private String id;
  private String name;
  private String description;

  private List<Review> reviews;
}
