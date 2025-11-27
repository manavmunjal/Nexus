package com.nexus.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductTest {

    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User("u1", "testuser", "user@test.com");
        product = new Product("p1", "Test Product", "Initial Description", "InitialCompany");
    }

    // ---- Getter & Setter Tests ----

    @Test
    void testGetId() {
        assertEquals("p1", product.getId());
    }

    @Test
    void testGetName() {
        assertEquals("Test Product", product.getName());
    }

    @Test
    void testSetName() {
        product.setName("New Name");
        assertEquals("New Name", product.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Initial Description", product.getDescription());
    }

    @Test
    void testSetDescription() {
        product.setDescription("Updated Description");
        assertEquals("Updated Description", product.getDescription());
    }

    @Test
    void testSetCompanyName() {
        product.setCompanyName("NewCompany");
        assertEquals("NewCompany", product.getCompanyName());
    }

    @Test
    void testSetRating() {
        product.setRating(4.5);
        assertEquals(4.5, product.getRating(), 0.001);
    }

    // ---- findAverageRating Tests ----

    @Test
    void testFindAverageRating_nullReviews() {
        double avg = product.findAverageRating(null);
        assertEquals(0.0, avg, 0.001);
        assertEquals(0.0, product.getRating(), 0.001);
    }

    @Test
    void testFindAverageRating_emptyReviews() {
        List<Review> emptyReviews = new ArrayList<>();
        double avg = product.findAverageRating(emptyReviews);
        assertEquals(0.0, avg, 0.001);
        assertEquals(0.0, product.getRating(), 0.001);
    }

    @Test
    void testFindAverageRating_nonEmptyReviews() {
        List<Review> reviews = new ArrayList<>();
        reviews.add(new Review("r1", "Good product", 3.0, user));
        reviews.add(new Review("r2", "Excellent", 5.0, user));
        reviews.add(new Review("r3", "Nice", 4.0, user));

        double avg = product.findAverageRating(reviews);
        assertEquals(4.0, avg, 0.001);
        assertEquals(4.0, product.getRating(), 0.001);
    }
}
