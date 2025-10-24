package com.nexus.controller;

import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.model.User;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ProductController productController;

  private Product testProduct;
  private Review testReview;
  private User testUser;

  @BeforeEach
  void setUp() {
      testProduct = new Product();
      testProduct.setId("1");
      testProduct.setName("Test Product");
      testProduct.setDescription("Test Description");
      testProduct.setReviewIds(new ArrayList<>());

      testUser = new User();
      testUser.setId("1");
      testUser.setUsername("testuser");
      testUser.setEmail("test@example.com");

      testReview = new Review();
      testReview.setId("1");
      testReview.setComment("Great product");
      testReview.setRating(5);
      testReview.setUser(testUser);
  }

  @Test
  void createProduct_ShouldSaveAndReturnProduct() {
      when(productRepository.save(any(Product.class))).thenReturn(testProduct);

      Product result = productController.createProduct(testProduct);

      assertNotNull(result);
      assertEquals("1", result.getId());
      assertEquals("Test Product", result.getName());
      verify(productRepository).save(testProduct);
  }

  @Test
  void getAllProducts_ShouldReturnListOfProducts() {
      List<Product> products = List.of(testProduct);
      when(productRepository.findAll()).thenReturn(products);

      List<Product> result = productController.getAllProducts();

      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(testProduct.getId(), result.get(0).getId());
      verify(productRepository).findAll();
  }

  @Test
  void postReview_ShouldSaveReviewAndUpdateProduct() {
      when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));
      when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
      when(productRepository.save(any(Product.class))).thenReturn(testProduct);

      Review result = productController.postReview("1", testReview);

      assertNotNull(result);
      assertEquals("1", result.getId());
      assertEquals("Great product", result.getComment());
      verify(reviewRepository).save(testReview);
      verify(productRepository).save(testProduct);
  }

  @Test
  void postReview_WithNewUser_ShouldSaveUserFirst() {
      User newUser = new User();
      newUser.setUsername("newuser");
      newUser.setEmail("new@example.com");
      
      Review reviewWithNewUser = new Review();
      reviewWithNewUser.setComment("Great product");
      reviewWithNewUser.setRating(5);
      reviewWithNewUser.setUser(newUser);

      when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));
      when(userRepository.save(any(User.class))).thenReturn(testUser);
      when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
      when(productRepository.save(any(Product.class))).thenReturn(testProduct);

      Review result = productController.postReview("1", reviewWithNewUser);

      assertNotNull(result);
      verify(userRepository).save(newUser);
      verify(reviewRepository).save(any(Review.class));
  }

  @Test
  void getReviews_ShouldReturnListOfReviews() {
      testProduct.setReviewIds(List.of("1", "2"));
      List<Review> reviews = List.of(testReview);

      when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));
      when(reviewRepository.findByIdIn(anyList())).thenReturn(reviews);

      List<Review> result = productController.getReviews("1");

      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals("Great product", result.get(0).getComment());
      verify(reviewRepository).findByIdIn(testProduct.getReviewIds());
  }

  @Test
  void updateReview_ShouldUpdateAndReturnReview() {
      Review updatedReview = new Review();
      updatedReview.setComment("Updated comment");
      updatedReview.setRating(4);

      when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));
      when(reviewRepository.findById("1")).thenReturn(Optional.of(testReview));
      when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

      Review result = productController.updateReview("1", "1", updatedReview);

      assertNotNull(result);
      verify(reviewRepository).save(any(Review.class));
  }
}