package com.nexus.repository;

import com.nexus.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
	List<Review> findByIdIn(List<String> ids);
}
