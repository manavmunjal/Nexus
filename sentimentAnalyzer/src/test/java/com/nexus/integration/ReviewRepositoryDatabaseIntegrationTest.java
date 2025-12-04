package com.nexus.integration;

import com.nexus.model.Review;
import com.nexus.model.User;
import com.nexus.repository.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ReviewRepository using embedded/in-memory MongoDB (Flapdoodle).
 *
 * <p>Tests saving, retrieving, and querying Review documents directly using the repository.
 * Uses User reference for validation.
 */
@DataMongoTest
class ReviewRepositoryDatabaseIntegrationTest {

  @Autowired
  private ReviewRepository reviewRepository;

  @AfterEach
  void tearDown() {
    reviewRepository.deleteAll();
  }

  /**
   * Tests saving a single review with a User and retrieving it by ID.
   */
  @Test
  void shouldSaveAndRetrieveReview() {
    // Dummy user
    User user = new User();
    user.setId("user-1");
    user.setUsername("johndoe");

    Review review = new Review();
    review.setRating(4.5);
    review.setComment("Great product!");
    review.setUser(user);

    // Save directly to the in-memory MongoDB
    Review savedReview = reviewRepository.save(review);

    assertThat(savedReview).isNotNull();
    assertThat(savedReview.getId()).isNotNull();
    assertThat(savedReview.getRating()).isEqualTo(4.5);
    assertThat(savedReview.getComment()).isEqualTo("Great product!");
    assertThat(savedReview.getUser()).isNotNull();
    assertThat(savedReview.getUser().getUsername()).isEqualTo("johndoe");

    // Retrieve by ID
    Optional<Review> fetched = reviewRepository.findById(savedReview.getId());
    assertThat(fetched).isPresent();
    assertThat(fetched.get().getId()).isEqualTo(savedReview.getId());
  }

  /**
   * Tests finding multiple reviews by a list of IDs.
   */
  @Test
  void shouldFindReviewsByIdIn() {
    User user = new User();
    user.setId("user-1");
    user.setUsername("alice");

    Review r1 = new Review();
    r1.setRating(5.0);
    r1.setComment("Excellent!");
    r1.setUser(user);

    Review r2 = new Review();
    r2.setRating(3.0);
    r2.setComment("Average");
    r2.setUser(user);

    Review r3 = new Review();
    r3.setRating(4.0);
    r3.setComment("Good");
    r3.setUser(user);

    reviewRepository.saveAll(Arrays.asList(r1, r2, r3));

    List<String> idsToFind = Arrays.asList(r1.getId(), r3.getId());
    List<Review> foundReviews = reviewRepository.findByIdIn(idsToFind);

    assertThat(foundReviews).hasSize(2);
    assertThat(foundReviews).extracting("id")
        .containsExactlyInAnyOrder(r1.getId(), r3.getId());
  }

  /**
   * Tests that querying with non-existent IDs returns an empty list.
   */
  @Test
  void shouldReturnEmptyListWhenIdsNotFound() {
    List<Review> reviews = reviewRepository.findByIdIn(Arrays.asList("non-existent-id"));
    assertThat(reviews).isEmpty();
  }
}
