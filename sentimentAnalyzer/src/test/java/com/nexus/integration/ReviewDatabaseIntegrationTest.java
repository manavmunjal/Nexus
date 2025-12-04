package com.nexus.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.controller.ProductController;
import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import com.nexus.sentiment.SentimentService;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests verifying Database Interactions via Mocks for Reviews (via
 * ProductController).
 */
@ExtendWith(MockitoExtension.class)
public class ReviewDatabaseIntegrationTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CompanyRepository companyRepository;

  @Mock
  private SentimentService sentimentService;

  @InjectMocks
  private ProductController productController;

  @Test
  public void testPostReview_CallsDatabaseSave() {
    String productId = "prod123";
    Product product = new Product();
    product.setId(productId);
    product.setReviewIds(new ArrayList<>());

    Review review = new Review();
    review.setComment("Great!");
    review.setRating(5.0);

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(reviewRepository.save(any(Review.class))).thenReturn(review);
    when(productRepository.save(any(Product.class))).thenReturn(product);

    productController.postReview(productId, review);

    verify(reviewRepository, times(1)).save(review);
    verify(productRepository, times(1)).save(product);
  }

  @Test
  public void testPostReview_DatabaseFailure_Returns500Error() {
    String productId = "prod123";
    Product product = new Product();
    product.setId(productId);

    Review review = new Review();

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(reviewRepository.save(any(Review.class)))
        .thenThrow(new DataAccessException("Connection refused") {
        });

    ResponseEntity<?> response = productController.postReview(productId, review);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }
}
