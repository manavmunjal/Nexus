package com.nexus.repository;

import com.nexus.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<Product, String> {}
