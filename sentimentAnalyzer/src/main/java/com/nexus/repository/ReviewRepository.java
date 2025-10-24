package com.nexus.repository;

import com.nexus.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository interface for Review entity operations.
 * Extends MongoRepository to provide CRUD operations for Review documents.
 */
public interface ReviewRepository extends MongoRepository<Review, String> {
  /**
   * Finds all reviews with IDs in the provided list.
   *
   * @param ids the list of review IDs to search for
   * @return a list of reviews matching the provided IDs
   */
  List<Review> findByIdIn(List<String> ids);
}
