package com.nexus.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexus.model.Review;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

@DataMongoTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class ReviewRepositoryEmbeddedTest {

  @Autowired
  private ReviewRepository reviewRepository;

  @BeforeEach
  void setUp() {
    reviewRepository.deleteAll();
  }

  @Test
  public void testSaveAndFindById() {
    Review review = new Review();
    review.setComment("Great product!");
    review.setRating(5.0);

    Review savedReview = reviewRepository.save(review);

    assertThat(savedReview.getId()).isNotNull();

    Optional<Review> foundReview = reviewRepository.findById(savedReview.getId());

    assertThat(foundReview).isPresent();
    assertThat(foundReview.get().getComment()).isEqualTo("Great product!");
    assertThat(foundReview.get().getRating()).isEqualTo(5.0);
  }

  @Test
  public void testFindByIdIn() {
    Review review1 = new Review();
    review1.setComment("Review 1");
    review1.setRating(4.0);

    Review review2 = new Review();
    review2.setComment("Review 2");
    review2.setRating(3.0);

    Review review3 = new Review();
    review3.setComment("Review 3");
    review3.setRating(2.0);

    reviewRepository.saveAll(Arrays.asList(review1, review2, review3));

    List<String> idsToFind = Arrays.asList(review1.getId(), review3.getId());

    List<Review> foundReviews = reviewRepository.findByIdIn(idsToFind);

    assertThat(foundReviews).hasSize(2);
    assertThat(foundReviews).extracting(Review::getId)
        .containsExactlyInAnyOrder(review1.getId(), review3.getId());
  }
}
