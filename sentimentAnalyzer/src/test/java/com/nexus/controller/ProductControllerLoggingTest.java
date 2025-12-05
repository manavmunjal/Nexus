package com.nexus.controller;

import com.nexus.auth.model.AuthUser;
import com.nexus.auth.service.UserAuthService;
import com.nexus.model.Company;
import com.nexus.model.Product;
import com.nexus.model.Review;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import com.nexus.repository.UserRepository;
import com.nexus.sentiment.SentimentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class ProductControllerLoggingTest {

    private ProductRepository productRepository;
    private ReviewRepository reviewRepository;
    private UserRepository userRepository;
    private CompanyRepository companyRepository;
    private SentimentService sentimentService;
    private UserAuthService userAuthService;
    private ProductController controller;
    private Product product;
    private Review review;
    private static final String VALID_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        userRepository = mock(UserRepository.class);
        companyRepository = mock(CompanyRepository.class);
        sentimentService = mock(SentimentService.class);
        userAuthService = mock(UserAuthService.class);

        controller = new ProductController(productRepository, reviewRepository,
                userRepository, companyRepository, sentimentService, userAuthService);

        product = new Product();
        product.setId("p1");
        product.setReviewIds(new ArrayList<>());

        review = new Review();
        review.setId("r1");

        when(userAuthService.validateUser(VALID_USER_ID)).thenReturn(new AuthUser(VALID_USER_ID));
    }

    @Test
    void createProduct_ShouldLogInfo_WhenRequestReceived(CapturedOutput output) {
        when(productRepository.save(product)).thenReturn(product);
        controller.createProduct(VALID_USER_ID, product);
        assertTrue(output.getOut().contains("Received request to create product"));
    }

    @Test
    void createProduct_ShouldLogError_WhenDatabaseException(CapturedOutput output) {
        when(productRepository.save(product)).thenThrow(new DataAccessException("DB down") {});
        controller.createProduct(VALID_USER_ID, product);
        assertTrue(output.getOut().contains("Database error while saving product"));
    }

    @Test
    void createProduct_ShouldLogInfo_WhenAssociatingWithCompany(CapturedOutput output) {
        Company company = new Company();
        company.setName("TestCo");
        company.setProducts(new ArrayList<>());

        product.setCompanyName("TestCo");

        when(productRepository.save(product)).thenReturn(product);
        when(companyRepository.findByName("TestCo")).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);

        controller.createProduct(VALID_USER_ID, product);

        assertTrue(output.getOut().contains("Associating product"));
    }

    @Test
    void postReview_ShouldLogInfo_WhenRequestReceived(CapturedOutput output) {
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
        when(productRepository.save(product)).thenReturn(product);

        controller.postReview(VALID_USER_ID, "p1", review);
        assertTrue(output.getOut().contains("Posting review for productId="));
    }

    @Test
    void postReview_ShouldLogWarn_WhenSentimentModelUntrained(CapturedOutput output) {
        review.setRating(0);
        review.setComment("Great product!");
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
        when(productRepository.save(product)).thenReturn(product);
        when(sentimentService.isTrained()).thenReturn(false);

        controller.postReview(VALID_USER_ID, "p1", review);

        assertTrue(output.getOut().contains("Sentiment model untrained"));
    }

    @Test
    void getReviews_ShouldLogInfo_WhenFetchingReviews(CapturedOutput output) {
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        controller.getReviews(VALID_USER_ID, "p1");
        assertTrue(output.getOut().contains("Fetching reviews for productId="));
    }

    @Test
    void updateReview_ShouldLogInfo_WhenUpdatingReview(CapturedOutput output) {
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        when(reviewRepository.findById("r1")).thenReturn(Optional.of(review));
        when(reviewRepository.findByIdIn(anyList())).thenReturn(List.of(review));
        when(productRepository.save(product)).thenReturn(product);
        when(reviewRepository.save(review)).thenReturn(review);

        controller.updateReview(VALID_USER_ID, "p1", "r1", review);

        assertTrue(output.getOut().contains("Updating review"));
    }

    @Test
    void getAverageRating_ShouldLogInfo_WhenFetchingAverageRating(CapturedOutput output) {
        product.setRating(4.5);
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));
        controller.getAverageRating(VALID_USER_ID, "p1");
        assertTrue(output.getOut().contains("Fetching average rating for productId="));
    }

    @Test
    void getAllProducts_ShouldLogInfo_WhenFetchingAllProducts(CapturedOutput output) {
        when(productRepository.findAll()).thenReturn(List.of(product));
        controller.getAllProducts(VALID_USER_ID);
        assertTrue(output.getOut().contains("Received request to fetch all products"));
    }
}
